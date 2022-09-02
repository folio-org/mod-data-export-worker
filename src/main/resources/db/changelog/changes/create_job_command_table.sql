CREATE TABLE IF NOT EXISTS job_command  (
	id UUID PRIMARY KEY,
	job_command_type VARCHAR(256),
	name VARCHAR(256),
	description TEXT,
	export_type VARCHAR(256),
	job_parameters JSONB,
	identifier_type VARCHAR(256),
	entity_type VARCHAR(256)
);
