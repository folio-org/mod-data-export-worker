package org.folio.dew.batch.acquisitions.services;

import org.folio.dew.client.OrganizationsClient;
import org.folio.dew.domain.dto.acquisitions.edifact.Organization;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationsService {
  private final OrganizationsClient organizationsClient;

  @Cacheable(cacheNames = "organizations", unless = "#result == null")
  public Organization getOrganizationById(String id) {
    return organizationsClient.getOrganizationById(id);
  }
}
