package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.client.OrganizationsClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrganizationsService {
  private final OrganizationsClient organizationsClient;

  public JsonNode getOrganizationById(String id) {
    return organizationsClient.getOrganizationById(id);
  }



}
