package org.folio.dew.repository;

import java.util.List;
import org.folio.de.entity.EHoldingsResource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EHoldingsResourceRepository extends CrudRepository<EHoldingsResource, EHoldingsResource.ResourceId> {
    @Query(value = "SELECT * FROM e_holdings_resource " +
      "WHERE job_execution_id = :jobExecutionId " +
      "AND (lower(name), id) > (:previousName, :previousId) " +
      "ORDER BY lower(name) ASC, id ASC " +
      "LIMIT :limit", nativeQuery = true)
  List<EHoldingsResource> seek(String previousName, String previousId, Long jobExecutionId, Integer limit);

  void deleteAllByJobExecutionId(Long jobExecutionId);
}
