package org.folio.dew.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.InstanceStatusesClient;
import org.folio.dew.client.InstanceModeOfIssuanceClient;
import org.folio.dew.client.InstanceTypesClient;
import org.folio.dew.client.NatureOfContentTermsClient;
import org.folio.dew.client.InstanceFormatsClient;
import org.folio.dew.client.IdentifierTypeClient;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.TypeOfIdentifiersCollection;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Service
@Log4j2
@RequiredArgsConstructor
public class InstanceReferenceService {

  private static final String QUERY_PATTERN_NAME = "name==\"%s\"";

  private final BulkEditProcessingErrorsService errorsService;
  private final InstanceStatusesClient instanceStatusesClient;
  private final InstanceModeOfIssuanceClient instanceModeOfIssuanceClient;
  private final InstanceTypesClient instanceTypesClient;
  private final NatureOfContentTermsClient natureOfContentTermsClient;
  private final InstanceFormatsClient instanceFormatsClient;
  private final IdentifierTypeClient identifierTypeClient;


  @Cacheable(cacheNames = "instanceStatusNames")
  public String getInstanceStatusNameById(String instanceStatusId, ErrorServiceArgs args) {
    try {
      return isEmpty(instanceStatusId) ? EMPTY : instanceStatusesClient.getById(instanceStatusId).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Instance status was not found by id: [%s]", instanceStatusId)), args.getFileName());
      return instanceStatusId;
    }
  }
  @Cacheable(cacheNames = "issuanceModeNames")
  public String getModeOfIssuanceNameById(String issuanceModeId, ErrorServiceArgs args) {
    try {
      return isEmpty(issuanceModeId) ? EMPTY : instanceModeOfIssuanceClient.getById(issuanceModeId).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Issuance mode was not found by id: [%s]", issuanceModeId)), args.getFileName());
      return issuanceModeId;
    }
  }
  @Cacheable(cacheNames = "instanceTypes")
  public String getInstanceTypeNameById(String instanceTypeId, ErrorServiceArgs args) {
    try {
      return isEmpty(instanceTypeId) ? EMPTY : instanceTypesClient.getById(instanceTypeId).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Instance type was not found by id: [%s]", instanceTypeId)), args.getFileName());
      return instanceTypeId;
    }
  }
  @Cacheable(cacheNames = "natureOfContentTermIds")
  public String getNatureOfContentTermNameById(String natureOfContentTermId, ErrorServiceArgs args) {
    try {
      return isEmpty(natureOfContentTermId) ? EMPTY : natureOfContentTermsClient.getById(natureOfContentTermId).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Nature of content term was not found by id: [%s]", natureOfContentTermId)), args.getFileName());
      return natureOfContentTermId;
    }
  }
  @Cacheable(cacheNames = "instanceFormatIds")
  public String getFormatOfInstanceNameById(String instanceFormatId, ErrorServiceArgs args) {
    try {
      return isEmpty(instanceFormatId) ? EMPTY : instanceFormatsClient.getById(instanceFormatId).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Instance format was not found by id: [%s]", instanceFormatId)), args.getFileName());
      return instanceFormatId;
    }
  }

  @Cacheable(cacheNames = "typeOfIdentifiersIds")
  public String getTypeOfIdentifiersIdByName(String identifierName) {
    if (StringUtils.isEmpty(identifierName)) {
      return null;
    }
    TypeOfIdentifiersCollection typeOfIdentifiers = identifierTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, identifierName));
    if (typeOfIdentifiers.getTypesOfIdentifier().isEmpty()) {
      log.error("Identifier type not found by identifierName={}", identifierName);
      return identifierName;
    }
    return typeOfIdentifiers.getTypesOfIdentifier().get(0).getId();
  }

}
