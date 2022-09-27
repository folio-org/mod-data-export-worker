package org.folio.dew.repository;

import java.util.List;

import org.folio.de.entity.EHoldingsResource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EHoldingsResourceRepository extends CrudRepository<EHoldingsResource, String> {
  @Query(value = "SELECT * FROM e_holdings_resource ORDER BY e_holdings_resource.id ASC LIMIT :limit1", nativeQuery = true)

  List<EHoldingsResource> seek(@Param("limit1") Integer limit1);
  @Query(value = "SELECT * FROM e_holdings_resource WHERE e_holdings_resource.id > :previousId ORDER BY e_holdings_resource.id ASC LIMIT :limit1", nativeQuery = true)
  List<EHoldingsResource> seek(@Param("previousId") String previousId, @Param("limit1") Integer limit1);

  void deleteAllByJobExecutionId(Long jobExecutionId);
}
