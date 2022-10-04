CREATE TABLE IF NOT EXISTS e_holdings_package (
    id VARCHAR(50) NOT NULL,
    job_execution_id BIGINT NOT NULL,
    e_package TEXT,
    e_provider TEXT,
    agreements TEXT,
    notes TEXT,
    PRIMARY KEY(job_execution_id, id)
);
