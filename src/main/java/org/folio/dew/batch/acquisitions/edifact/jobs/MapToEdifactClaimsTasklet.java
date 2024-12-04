package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.utils.QueryUtils.convertIdsToCqlQuery;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.dew.batch.acquisitions.edifact.PurchaseOrdersToEdifactMapper;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.acquisitions.edifact.EdifactExportHolder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Component
@StepScope
@Log4j2
public class MapToEdifactClaimsTasklet extends MapToEdifactTasklet {

  public static final String CLAIM_PIECE_IDS = "claimPieceIds";

  public MapToEdifactClaimsTasklet(ObjectMapper ediObjectMapper, OrdersService ordersService,
                                   PurchaseOrdersToEdifactMapper purchaseOrdersToEdifactMapper) {
    super(ediObjectMapper, ordersService, purchaseOrdersToEdifactMapper);
  }

  protected List<String> getExportConfigMissingFields(VendorEdiOrdersExportConfig ediOrdersExportConfig) {
    return CollectionUtils.isEmpty(ediOrdersExportConfig.getClaimPieceIds())
      ? List.of(CLAIM_PIECE_IDS)
      : List.of();
  }

  @Override
  protected EdifactExportHolder buildEdifactExportHolder(ChunkContext chunkContext, VendorEdiOrdersExportConfig ediExportConfig, Map<String, Object> jobParameters) {
    var pieces = ordersService.getPiecesByIdsAndReceivingStatus(ediExportConfig.getClaimPieceIds(), Piece.ReceivingStatusEnum.LATE);
    var poLineQuery = convertIdsToCqlQuery(pieces.stream().map(Piece::getPoLineId).toList());
    var compOrders = getCompositeOrders(poLineQuery);
    return new EdifactExportHolder(compOrders, pieces);
  }

}
