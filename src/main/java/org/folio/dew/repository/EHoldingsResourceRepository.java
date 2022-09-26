package org.folio.dew.repository;

import java.util.List;

import org.folio.de.entity.EHoldingsResource;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EHoldingsResourceRepository extends CrudRepository<EHoldingsResource, String> {
  List<EHoldingsResource> findByResourceId(String resourceId);

  void deleteAllByJobExecutionId(Long jobExecutionId);
}
