# Chronos

A delayed task scheduler. You POST a task with a time in the future, it gets executed then. Failures are retried with backoff, and tasks abandoned by a crashed instance get picked back up.

Built with Spring Boot and PostgreSQL.

## Why not just a cron job

The usual way to do this is a scheduled method that scans a table for rows that are due. That works until you run a second instance. Both poll, both read the same PENDING row before either writes a status back, and the task runs twice.

`SELECT ... FOR UPDATE` stops the double execution but makes the instances queue behind each other. `FOR UPDATE SKIP LOCKED` skips rows another transaction already holds instead of waiting on them, so instances split the work rather than taking turns.

That's the claim query:

```sql
SELECT * FROM delayed_tasks
WHERE status = 'PENDING' AND execute_at <= :now
ORDER BY execute_at
LIMIT :batchSize
FOR UPDATE SKIP LOCKED
```

It runs in one transaction with the status update, so the lock is held for as short a time as possible.

## Retries and recovery

A failed task goes back to PENDING with a delay of `base * multiplier^(retryCount - 1)`. Defaults give 30s, 60s, 120s, then FAILED.

Backoff doesn't help if the process dies mid-execution. The task sits in PROCESSING, nothing holds a lock on it, and no poller will claim it again. So a second job sweeps tasks that have been PROCESSING past a timeout and runs them through the same failure path. A crash costs one retry, not the task.

## Task Lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING: submitted
    PENDING --> PROCESSING: claimed by poller
    PROCESSING --> DONE: executor succeeded
    PROCESSING --> PENDING: failed, retry after backoff
    PROCESSING --> FAILED: retries exhausted
    PROCESSING --> PENDING: stuck past timeout, recovered
    DONE --> [*]
    FAILED --> [*]
```

## Running it

You only need Docker. The build happens inside the image, so there's no need for Java, Maven or PostgreSQL on the host.

```bash
git clone <repo-url>
cd deferred-task-processor
cp .env.example .env     # then fill in real values
docker compose up --build
```

`.env` is gitignored so it won't come with the clone. `DB_USERNAME` and `DB_PASSWORD` can be anything — Compose creates the database with whatever you set. `MAIL_USERNAME` and `MAIL_PASSWORD` need a real Gmail address and app password, or the email executor has nothing to send through.

The app comes up on `localhost:8080`, Postgres on `localhost:5433` — 5433 rather than 5432 so it doesn't clash with a local Postgres install. The app waits on the database's health check before starting, since it won't retry its first connection.

```bash
docker compose logs -f app   # follow the scheduler
docker compose down          # stop, keep the data
docker compose down -v       # stop, wipe the database
```

To run it outside a container you need Java 21, Maven and a PostgreSQL instance, with the same four variables exported into the environment.

## How the image is built

Two stages. The first builds the jar with Maven on a JDK image, the second copies the result onto a JRE-only image and runs as a non-root user — no compiler, build tool or source in the final image.

The jar isn't copied in as one file. It's unpacked into layers (dependencies, loader, snapshot dependencies, application) and copied in that order, so changing a line of Java only invalidates the last and smallest layer. `pom.xml` is also copied and resolved before `src`, so a code change doesn't re-download the dependency tree.

## API

The REST API is served under `/api/tasks`.

### Submit a task

```http
POST /api/tasks
Content-Type: application/json
```

```json
{
  "payload": {
    "to": "recipient@example.com",
    "subject": "Reminder",
    "body": "Your scheduled reminder"
  },
  "executeAt": "2027-09-23T12:00:00+05:30"
}
```

`executeAt` is an instant, so it must carry a zone offset — `+05:30` or `Z`. A timestamp without one is rejected, because there'd be no way to know which moment was meant. `payload` is any JSON object; only the executor cares what's in it, and the email one wants `to`, `subject` and `body`.

The response status is `201 Created`. Timestamps come back in UTC.

```json
{
  "id": 1,
  "payload": {
    "to": "recipient@example.com",
    "subject": "Reminder",
    "body": "Your scheduled reminder"
  },
  "executeAt": "2027-09-23T06:30:00Z",
  "status": "PENDING",
  "retryCount": 0,
  "maxRetries": 3,
  "createdAt": "2026-10-24T07:46:00Z"
}
```

### Get a task

```http
GET /api/tasks/{id}
```

The response status is `200 OK` and uses the same task response shape. A missing task returns `404 Not Found`; invalid request fields return `400 Bad Request` with field errors.

Swagger UI is available at `/api/docs`.

## Configuration

| Property                                 | Default | Notes                                  |
| ---------------------------------------- | ------- | -------------------------------------- |
| `chronos.scheduler.poll-interval-ms`     | `5000`  | delay between polls                    |
| `chronos.scheduler.batch-size`           | `10`    | tasks claimed per poll                 |
| `chronos.scheduler.recovery-interval-ms` | `60000` | delay between recovery sweeps          |
| `chronos.scheduler.stuck-task-timeout`   | `2m`    | how long PROCESSING is allowed to last |
| `chronos.retry.max-retries`              | `3`     | retry budget for new tasks             |
| `chronos.retry.backoff-base-seconds`     | `30`    | first retry delay                      |
| `chronos.retry.backoff-multiplier`       | `2`     | growth per retry                       |

Credentials come from env vars: `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`.

## Tech Stack

- Java 21
- Spring Boot 4.0.8
- Spring Data JPA
- Spring MVC
- Spring Mail
- PostgreSQL 17
- Springdoc OpenAPI
- Lombok
- Maven
- Docker / Docker Compose

## Known limitations

- A claimed batch executes sequentially on the scheduler thread, so one slow task holds up the rest of its batch.
- Permanent failures like a malformed payload burn the full retry budget the same as an SMTP timeout would.
- No auth on the API.
- No tests yet apart from the generated context-load one.
