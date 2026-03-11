package org.folio.dew.batch.acquisitions.services;

import org.folio.dew.client.OrganizationsClient;
import org.folio.dew.domain.dto.acquisitions.edifact.Organization;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrganizationsService {
  private final OrganizationsClient organizationsClient;

  public Organization getOrganizationById(String id) {
    return organizationsClient.getOrganizationById(id);
  }



}
