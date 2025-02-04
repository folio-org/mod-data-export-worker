package org.folio.dew.batch.acquisitions.jobs;

import static org.folio.dew.batch.acquisitions.services.OrdersService.CHUNK_SIZE;
import static org.folio.dew.batch.acquisitions.utils.ExportConfigFields.CLAIM_PIECE_IDS;
import static org.folio.dew.batch.acquisitions.utils.ExportConfigFields.LIB_EDI_TYPE;
import static org.folio.dew.batch.acquisitions.utils.ExportConfigFields.VENDOR_EDI_TYPE;
import static org.folio.dew.batch.acquisitions.utils.ExportUtils.validateField;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.FileFormatEnum.EDI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.dew.batch.acquisitions.mapper.ExportResourceMapper;
import org.folio.dew.batch.acquisitions.services.OrdersService;
import org.folio.dew.batch.acquisitions.services.OrganizationsService;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
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

    var compOrdersMap = StreamEx.ofSubLists(pieces.stream().map(Piece::getPoLineId).distinct().toList(), CHUNK_SIZE)
      .map(QueryUtils::convertIdsToCqlQuery)
      .map(this::getCompositeOrders)
      .flatMap(Collection::stream)
      .groupingBy(CompositePurchaseOrder::getId);

    // Composite orders are fetched in chunks based on po lines, so there can be multiple orders with the same id
    // and different po lines in the map values. We need to merge them into one order with all po lines.
    var compOrders = compOrdersMap.values().stream()
      .filter(CollectionUtils::isNotEmpty)
      .map(orders -> {
        var poLines = orders.stream()
          .map(CompositePurchaseOrder::getCompositePoLines)
          .flatMap(Collection::stream)
          .toList();
        return orders.get(0).compositePoLines(poLines);
      }).toList();

    return new ExportHolder(compOrders, pieces);
  }

}
