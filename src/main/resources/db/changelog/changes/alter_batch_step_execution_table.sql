ALTER TABLE BATCH_STEP_EXECUTION
ADD CREATE_TIME TIMESTAMP NOT NULL,
ALTER COLUMN START_TIME DROP NOT NULL,
ALTER COLUMN START_TIME SET DEFAULT null;