# Deferred Task Processor

A lightweight, SQS-style delayed task queue with built-in retry logic.

---

## Overview

A lightweight, distributed task scheduler built for reliable delayed execution. It safely coordinates scheduled jobs across multiple instances with automatic retries and concurrency safety. 
While the reference implementation handles delayed email dispatch via Gmail SMTP, the core worker interface makes it plug-and-play for webhooks, report processing, or custom API workflows.

---

##  Tech Stack

- Java 21
- Spring Boot 4.0.8
- PostgreSQL 
- Spring Data JPA
- Maven