package org.folio.dew.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.client.InstanceClient;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.domain.dto.HoldingsRecord;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Log4j2
public class HoldingsReferenceService extends FolioExecutionContextManager {
  private static final String QUERY_PATTERN_HRID = "hrid==\"%s\"";
  private static final String QUERY_PATTERN_BARCODE = "barcode==\"%s\"";

  private final InstanceClient instanceClient;
  private final InventoryClient inventoryClient;
  private final BulkEditProcessingErrorsService errorsService;
  private final HoldingClient holdingClient;
  private final FolioExecutionContext folioExecutionContext;
  private final HoldingsReferenceServiceCache holdingsReferenceServiceCache;

  public HoldingsRecord getHoldingById(String id, String tenantId) {
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return holdingClient.getHoldingsRecordById(id);
    } catch (Exception e) {
      var msg = "Holding not found by id=" + id;
      log.error(msg);
      return null;
    }
  }

  public String getInstanceIdByHrid(String instanceHrid) {
    var briefInstances = instanceClient.getByQuery(String.format(QUERY_PATTERN_HRID, instanceHrid));
    if (briefInstances.getInstances().isEmpty()) {
      throw new BulkEditException("Instance not found by hrid=" + instanceHrid, ErrorType.WARNING);
    } else {
      return briefInstances.getInstances().get(0).getId();
    }
  }

  public String getInstanceTitleById(String instanceId, String tenantId) {
    if (isEmpty(instanceId)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      var instanceJson = instanceClient.getInstanceJsonById(instanceId);
      var title = instanceJson.get("title");
      var publications = instanceJson.get("publication");
      String publicationString = EMPTY;
      if (nonNull(publications) && publications.isArray() && !publications.isEmpty()) {
        publicationString = formatPublication(publications.get(0));
      }
      return title.asText() + publicationString;
    } catch (NotFoundException e) {
      var msg = "Instance not found by id=" + instanceId;
      log.error(msg);
      throw new BulkEditException(msg, ErrorType.WARNING);
    }
  }

  private String formatPublication(JsonNode publication) {
    if (nonNull(publication)) {
      var publisher = publication.get("publisher");
      var dateOfPublication = publication.get("dateOfPublication");
      if (isNull(dateOfPublication)) {
        return isNull(publisher) ? EMPTY : String.format(". %s", publisher.asText());
      }
      return String.format(". %s, %s", isNull(publisher) ? EMPTY : publisher.asText(), dateOfPublication.asText());
    }
    return EMPTY;
  }

  public String getHoldingsIdByItemBarcode(String itemBarcode) {
    var items = inventoryClient.getItemByQuery(String.format(QUERY_PATTERN_BARCODE, itemBarcode), 1);
    if (items.getItems().isEmpty()) {
      throw new BulkEditException("Item not found by barcode=" + itemBarcode, ErrorType.WARNING);
    }
    return items.getItems().get(0).getHoldingsRecordId();
  }

  public String getHoldingsTypeNameById(String id, ErrorServiceArgs args, String tenantId) {
    try  {
      return holdingsReferenceServiceCache.getHoldingsTypeNameById(id, tenantId);
    } catch (NotFoundException e) {
      var msg = "Holdings type not found by id=" + id;
      log.error(msg);
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(msg, ErrorType.WARNING), args.getFileName(), ErrorType.WARNING);
      return id;
    }
  }

  public String getCallNumberTypeNameById(String id, ErrorServiceArgs args, String tenantId) {
    try  {
      return holdingsReferenceServiceCache.getCallNumberTypeNameById(id, tenantId);
    } catch (NotFoundException e) {
      var msg = "Call number type not found by id=" + id;
      log.error(msg);
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(msg, ErrorType.WARNING), args.getFileName(), ErrorType.WARNING);
      return id;
    }
  }

  public String getNoteTypeNameById(String id, ErrorServiceArgs args, String tenantId) {
    try {
      return holdingsReferenceServiceCache.getNoteTypeNameById(id, tenantId);
    } catch (NotFoundException e) {
      var msg = "Note type not found by id=" + id;
      log.error(msg);
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(msg, ErrorType.WARNING), args.getFileName(), ErrorType.WARNING);
      return id;
    }
  }

  public String getIllPolicyNameById(String id, ErrorServiceArgs args, String tenantId) {
    try {
      return holdingsReferenceServiceCache.getIllPolicyNameById(id, tenantId);
    } catch (NotFoundException e) {
      var msg = "Ill policy not found by id=" + id;
      log.error(msg);
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(msg, ErrorType.WARNING), args.getFileName(), ErrorType.WARNING);
      return id;
    }
  }


  public String getSourceNameById(String id, ErrorServiceArgs args, String tenantId) {
    try {
      return holdingsReferenceServiceCache.getSourceNameById(id, tenantId);
    } catch (NotFoundException e) {
      var msg = "Holdings record source not found by id=" + id;
      log.error(msg);
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(msg, ErrorType.WARNING), args.getFileName(), ErrorType.WARNING);
      return id;
    }
  }

  public String getStatisticalCodeNameById(String id, ErrorServiceArgs args, String tenantId) {
    try {
      return holdingsReferenceServiceCache.getStatisticalCodeNameById(id, tenantId);
    } catch (NotFoundException e) {
      var msg = "Statistical code not found by id=" + id;
      log.error(msg);
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(msg, ErrorType.WARNING), args.getFileName(), ErrorType.WARNING);
      return id;
    }
  }

  public JsonNode getHoldingsJsonById(String holdingsId, String tenantId) {
    return  holdingsReferenceServiceCache.getHoldingsJsonById(holdingsId, tenantId);
  }

  public JsonNode getHoldingsLocationById(String locationId, String tenantId) {
   return  holdingsReferenceServiceCache.getHoldingsLocationById(locationId, tenantId);
  }

  public String getLocationNameById(String id, String tenantId) {
   return holdingsReferenceServiceCache.getLocationNameById(id, tenantId);
  }
}
