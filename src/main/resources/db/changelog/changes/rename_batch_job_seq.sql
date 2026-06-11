-- Spring Batch 6.0 renamed the job-instance sequence from BATCH_JOB_SEQ to BATCH_JOB_INSTANCE_SEQ.
-- See the official migration script:
-- https://github.com/spring-projects/spring-batch/blob/main/spring-batch-core/src/main/resources/org/springframework/batch/core/migration/6.0/migration-postgresql.sql
ALTER SEQUENCE IF EXISTS BATCH_JOB_SEQ RENAME TO BATCH_JOB_INSTANCE_SEQ;

