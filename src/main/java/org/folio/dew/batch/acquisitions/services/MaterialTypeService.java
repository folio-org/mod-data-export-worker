package org.folio.dew.batch.acquisitions.services;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.MaterialTypeClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import static java.util.Objects.isNull;

@Log4j2
@Service
@RequiredArgsConstructor
public class MaterialTypeService {
  private final MaterialTypeClient materialTypeClient;

  @Cacheable(cacheNames = "materialTypeNames")
  public String getMaterialTypeName(String id) {
    var materialType = materialTypeClient.getMaterialType(id);
    return isNull(materialType) ? "" : materialType.getName();
  }
}
