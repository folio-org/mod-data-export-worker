package org.folio.dew.repository;

import java.util.List;

import org.folio.de.entity.EHoldingsResource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EHoldingsResourceRepository extends CrudRepository<EHoldingsResource, EHoldingsResource.ResourceId> {
    @Query(value = "SELECT * FROM e_holdings_resource " +
      "WHERE name > (coalesce((SELECT name FROM e_holdings_resource WHERE id = :previousId " +
      "AND job_execution_id = :jobExecutionId), '')) " +
      "AND job_execution_id = :jobExecutionId " +
      "ORDER BY name ASC LIMIT :limit", nativeQuery = true)
  List<EHoldingsResource> seek(String previousId, Long jobExecutionId, Integer limit);

  void deleteAllByJobExecutionId(Long jobExecutionId);
}
