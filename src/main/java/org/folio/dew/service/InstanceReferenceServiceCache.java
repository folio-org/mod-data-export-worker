package org.folio.dew.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.IdentifierTypeClient;
import org.folio.dew.client.InstanceFormatsClient;
import org.folio.dew.client.InstanceModeOfIssuanceClient;
import org.folio.dew.client.InstanceNoteTypesClient;
import org.folio.dew.client.InstanceStatusesClient;
import org.folio.dew.client.InstanceTypesClient;
import org.folio.dew.client.NatureOfContentTermsClient;
import org.folio.dew.client.StatisticalCodeClient;
import org.folio.dew.domain.dto.IdentifierTypeReferenceCollection;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Service
@Log4j2
@RequiredArgsConstructor
public class InstanceReferenceServiceCache {

  private static final String QUERY_PATTERN_NAME = "name==\"%s\"";

  private final InstanceStatusesClient instanceStatusesClient;
  private final InstanceModeOfIssuanceClient instanceModeOfIssuanceClient;
  private final InstanceTypesClient instanceTypesClient;
  private final NatureOfContentTermsClient natureOfContentTermsClient;
  private final InstanceFormatsClient instanceFormatsClient;
  private final IdentifierTypeClient identifierTypeClient;
  private final InstanceNoteTypesClient instanceNoteTypesClient;
  private final StatisticalCodeClient statisticalCodeClient;


  @Cacheable(cacheNames = "instanceStatusNames")
  public String getInstanceStatusNameById(String instanceStatusId) {
      return isEmpty(instanceStatusId) ? EMPTY : instanceStatusesClient.getById(instanceStatusId).getName();
  }

  @Cacheable(cacheNames = "issuanceModeNames")
  public String getModeOfIssuanceNameById(String issuanceModeId) {
      return isEmpty(issuanceModeId) ? EMPTY : instanceModeOfIssuanceClient.getById(issuanceModeId).getName();
  }

  @Cacheable(cacheNames = "instanceTypes")
  public String getInstanceTypeNameById(String instanceTypeId) {
      return isEmpty(instanceTypeId) ? EMPTY : instanceTypesClient.getById(instanceTypeId).getName();
  }

  @Cacheable(cacheNames = "natureOfContentTermIds")
  public String getNatureOfContentTermNameById(String natureOfContentTermId) {
      return isEmpty(natureOfContentTermId) ? EMPTY : natureOfContentTermsClient.getById(natureOfContentTermId).getName();
  }

  @Cacheable(cacheNames = "instanceFormatIds")
  public String getFormatOfInstanceNameById(String instanceFormatId) {
      return isEmpty(instanceFormatId) ? EMPTY : instanceFormatsClient.getById(instanceFormatId).getName();
  }

  @Cacheable(cacheNames = "typeOfIdentifiersIds")
  public String getTypeOfIdentifiersIdByName(String identifierName) {
    if (StringUtils.isEmpty(identifierName)) {
      return null;
    }
    IdentifierTypeReferenceCollection typeOfIdentifiers = identifierTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, identifierName));
    if (typeOfIdentifiers.getIdentifierTypes().isEmpty()) {
      log.error("Identifier type not found by identifierName={}", identifierName);
      return identifierName;
    }
    return typeOfIdentifiers.getIdentifierTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "instanceNoteTypes")
  public String getInstanceNoteTypeNameById(String noteTypeId) {
      return isEmpty(noteTypeId) ? EMPTY : instanceNoteTypesClient.getNoteTypeById(noteTypeId).getName();
  }

  @Cacheable(cacheNames = "instanceStatisticalCodeNames")
  public String getStatisticalCodeNameById(String id) {
    if (StringUtils.isEmpty(id)) {
      return EMPTY;
    }
    return statisticalCodeClient.getById(id).getName();
  }
}
