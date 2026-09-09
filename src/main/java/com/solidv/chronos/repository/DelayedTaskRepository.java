package com.solidv.chronos.repository;

import com.solidv.chronos.entity.DelayedTask;
import com.solidv.chronos.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DelayedTaskRepository extends JpaRepository<DelayedTask, Long> {

    List<DelayedTask> findByStatusAndExecuteAtLessThanEqual(TaskStatus status, LocalDateTime executeAt);

    List<DelayedTask> findByStatusAndUpdatedAtLessThan(TaskStatus status, LocalDateTime cutoff);

    /**
     * Finds and locks a batch of pending tasks that are due for execution.
     * SKIP LOCKED is used to prevent multiple instances from blocking each other.
     * If a row is already locked by another transaction, it is skipped, allowing
     * concurrent instances to process different tasks simultaneously.
     */
    @Query(value = """
            SELECT * FROM delayed_tasks
            WHERE status = 'PENDING' AND execute_at <= :now
            ORDER BY execute_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<DelayedTask> findTasksToClaim(@Param("now") LocalDateTime now, @Param("batchSize") int batchSize);
}
