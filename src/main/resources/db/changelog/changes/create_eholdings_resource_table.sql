CREATE TABLE IF NOT EXISTS e_holdings_resource (
    id VARCHAR(50) NOT NULL,
    job_execution_id BIGINT NOT NULL,
    resources_data TEXT,
    agreements TEXT,
    notes TEXT,
    PRIMARY KEY(job_execution_id, id)
);
