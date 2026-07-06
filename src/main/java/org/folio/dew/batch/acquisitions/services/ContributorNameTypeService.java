package org.folio.dew.batch.acquisitions.services;

import org.folio.dew.client.ContributorNameTypeClient;
import org.folio.dew.domain.dto.acquisitions.edifact.ContributorNameType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContributorNameTypeService {

  private final ContributorNameTypeClient contributorNameTypeClient;

  @Cacheable(cacheNames = "contributorNameTypes", unless = "#result == null")
  public String getContributorNameTypeName(String id) {
    ContributorNameType contributorNameType = contributorNameTypeClient.getContributorNameType(id);
    return contributorNameType != null ? contributorNameType.getName() : "";
  }
}
