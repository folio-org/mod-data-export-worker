package org.folio.dew.batch.acquisitions.jobs;

import static org.folio.dew.batch.acquisitions.services.OrdersService.CHUNK_SIZE;
import static org.folio.dew.batch.acquisitions.utils.ExportConfigFields.CLAIM_PIECE_IDS;
import static org.folio.dew.batch.acquisitions.utils.ExportConfigFields.LIB_EDI_TYPE;
import static org.folio.dew.batch.acquisitions.utils.ExportConfigFields.VENDOR_EDI_TYPE;
import static org.folio.dew.batch.acquisitions.utils.ExportUtils.validateField;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.FileFormatEnum.EDI;
import static org.folio.dew.utils.QueryUtils.convertIdsToCqlQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.dew.batch.acquisitions.mapper.ExportResourceMapper;
import org.folio.dew.batch.acquisitions.services.OrdersService;
import org.folio.dew.batch.acquisitions.services.OrganizationsService;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.acquisitions.edifact.ExportHolder;
import org.folio.dew.error.NotFoundException;
import org.folio.dew.utils.QueryUtils;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import one.util.streamex.StreamEx;

@Component
@StepScope
public class MapToEdifactClaimsTasklet extends MapToEdifactTasklet {

  private final ExportResourceMapper edifactMapper;
  private final ExportResourceMapper csvMapper;

  public MapToEdifactClaimsTasklet(ObjectMapper ediObjectMapper, OrganizationsService organizationsService, OrdersService ordersService,
                                   ExportResourceMapper edifactMapper, ExportResourceMapper csvMapper) {
    super(ediObjectMapper, organizationsService, ordersService);
    this.edifactMapper = edifactMapper;
    this.csvMapper = csvMapper;
  }

  @Override
  protected ExportResourceMapper getExportResourceMapper(VendorEdiOrdersExportConfig ediOrdersExportConfig) {
    return switch (ediOrdersExportConfig.getFileFormat()) {
      case EDI -> edifactMapper;
      case CSV -> csvMapper;
    };
  }

  @Override
  protected List<String> getExportConfigMissingFields(VendorEdiOrdersExportConfig ediOrdersExportConfig) {
    List<String> missingFields = new ArrayList<>();
    validateField(CLAIM_PIECE_IDS.getName(), ediOrdersExportConfig.getClaimPieceIds(), CollectionUtils::isNotEmpty, missingFields);

    if (ediOrdersExportConfig.getFileFormat() == EDI) {
      var ediConfig = ediOrdersExportConfig.getEdiConfig();
      validateField(LIB_EDI_TYPE.getName(), ediConfig.getLibEdiType(), Objects::nonNull, missingFields);
      validateField(VENDOR_EDI_TYPE.getName(), ediConfig.getVendorEdiType(), Objects::nonNull, missingFields);
    }
    return missingFields;
  }

  @Override
  protected ExportHolder buildEdifactExportHolder(VendorEdiOrdersExportConfig ediExportConfig, Map<String, Object> jobParameters) {
    var pieces = ordersService.getPiecesByIdsAndReceivingStatus(ediExportConfig.getClaimPieceIds(), Piece.ReceivingStatusEnum.CLAIM_SENT);
    if (pieces.isEmpty()) {
      throw new NotFoundException(Piece.class);
    }

    var compOrders = StreamEx.ofSubLists(pieces.stream().map(Piece::getPoLineId).toList(), CHUNK_SIZE)
      .map(QueryUtils::convertIdsToCqlQuery)
      .map(this::getCompositeOrders)
      .flatMap(Collection::stream)
      .toList();
    return new ExportHolder(compOrders, pieces);
  }

}
