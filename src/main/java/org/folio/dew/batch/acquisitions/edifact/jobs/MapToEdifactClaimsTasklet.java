package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.utils.QueryUtils.convertIdsToCqlQuery;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.dew.batch.acquisitions.edifact.mapper.EdifactMapper;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.acquisitions.edifact.ExportHolder;
import org.folio.dew.error.NotFoundException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@StepScope
public class MapToEdifactClaimsTasklet extends MapToEdifactTasklet {

  public static final String CLAIM_PIECE_IDS = "claimPieceIds";

  public MapToEdifactClaimsTasklet(ObjectMapper ediObjectMapper, OrdersService ordersService,
                                   EdifactMapper edifactMapper) {
    super(ediObjectMapper, ordersService, edifactMapper);
  }

  protected List<String> getExportConfigMissingFields(VendorEdiOrdersExportConfig ediOrdersExportConfig) {
    return CollectionUtils.isEmpty(ediOrdersExportConfig.getClaimPieceIds())
      ? List.of(CLAIM_PIECE_IDS)
      : List.of();
  }

  @Override
  protected ExportHolder buildEdifactExportHolder(ChunkContext chunkContext, VendorEdiOrdersExportConfig ediExportConfig, Map<String, Object> jobParameters) {
    var pieces = ordersService.getPiecesByIdsAndReceivingStatus(ediExportConfig.getClaimPieceIds(), Piece.ReceivingStatusEnum.LATE);
    if (pieces.isEmpty()) {
      throw new NotFoundException(Piece.class);
    }

    var poLineQuery = convertIdsToCqlQuery(pieces.stream().map(Piece::getPoLineId).toList());
    var compOrders = getCompositeOrders(poLineQuery);
    return new ExportHolder(compOrders, pieces);
  }

}
