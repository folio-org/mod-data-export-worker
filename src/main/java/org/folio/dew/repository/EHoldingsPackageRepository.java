package org.folio.dew.repository;

import org.folio.de.entity.EHoldingsPackage;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EHoldingsPackageRepository extends CrudRepository<EHoldingsPackage, EHoldingsPackage.PackageId> {
  void deleteAllByJobExecutionId(Long jobExecutionId);
}
