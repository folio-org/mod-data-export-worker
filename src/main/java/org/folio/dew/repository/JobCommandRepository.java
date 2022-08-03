package org.folio.dew.repository;

import org.folio.de.entity.JobCommand;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobCommandRepository extends CrudRepository<JobCommand, UUID> {
}
