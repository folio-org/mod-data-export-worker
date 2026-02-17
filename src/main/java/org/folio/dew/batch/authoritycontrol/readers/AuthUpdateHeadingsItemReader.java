package org.folio.dew.batch.authoritycontrol.readers;

import static org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto.ActionEnum.UPDATE_HEADING;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.EntitiesLinksStatsClient;
import org.folio.dew.config.properties.AuthorityControlJobProperties;
import org.folio.dew.domain.dto.authority.control.AuthorityControlExportConfig;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDtoCollection;
import org.folio.dew.service.FolioTenantService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@Log4j2
@StepScope
@Component
public class AuthUpdateHeadingsItemReader extends AuthorityControlItemReader<AuthorityDataStatDto> {

  private final FolioExecutionContext context;
  private final FolioExecutionContextService executionService;
  private final String consortiumTenant;

  private List<AuthorityDataStatDto> overflowStats;

  public AuthUpdateHeadingsItemReader(EntitiesLinksStatsClient entitiesLinksStatsClient,
                                      AuthorityControlExportConfig exportConfig,
                                      AuthorityControlJobProperties jobProperties,
                                      FolioTenantService folioTenantService,
                                      FolioExecutionContext context,
                                      FolioExecutionContextService executionService) {
    super(entitiesLinksStatsClient, exportConfig, jobProperties);
    this.context = context;
    this.executionService = executionService;
    this.consortiumTenant = folioTenantService.getConsortiumTenant();
  }

  @Override
  protected AuthorityDataStatDtoCollection getCollection(int limit) {
    log.debug("Fetching authority stats for tenant [{}] ", context.getTenantId());
    if (isConsortiumMemberTenant()) {
      return getConsortiumAuthorityStats(limit);
    }
    if (toDate() == null) {
      return null;
    }
    if (isConsortiumTenant()) {
      return getAuthorityStatsFromCentralTenant(limit);
    }
    return entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toDate());
  }

  @Override
  protected AuthorityDataStatDto doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.size()) {
      if (toDate == null && toConsortiumDate == null && currentChunk != null) {
        var collection = getCollection(limit);
        if (collection == null || collection.getStats() == null || collection.getStats().isEmpty()) {
          return null;
        }
        currentChunk = collection.getStats();
        currentChunkOffset = 0;
      } else {
        var collection = getCollection(limit);
        currentChunk = collection.getStats();
        toDate = collection.getNext();
        toConsortiumDate = collection.getConsortiumNext();
        currentChunkOffset = 0;
      }
    }

    if (currentChunk.isEmpty()) {
      return null;
    }
    return currentChunk.get(currentChunkOffset++);
  }

  private AuthorityDataStatDtoCollection getAuthorityStatsFromCentralTenant(int limit) {
    var result = entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toDate());
    if (result != null && result.getStats() != null && !result.getStats().isEmpty()) {
      result.getStats().forEach(stat -> stat.setShared(true));
    }
    return result;
  }

  private AuthorityDataStatDtoCollection getConsortiumAuthorityStats(int limit) {
    var memberTenantStats = fetchMemberTenantStats(limit);
    var centralTenantStats = fetchCentralTenantStats(limit);

    if (memberTenantStats == null && centralTenantStats == null) {
      return getDataFromOverflowStats(limit);
    }
    var mergedStats = mergeStats(memberTenantStats, centralTenantStats);
    if (mergedStats != null && mergedStats.size() > limit) {
      return createStatsPage(limit, mergedStats, memberTenantStats, centralTenantStats);
    }
    return getMergedAuthorityStats(mergedStats, memberTenantStats, centralTenantStats);
  }

  private boolean isConsortiumMemberTenant() {
    return consortiumTenant != null && !consortiumTenant.equals(context.getTenantId());
  }

  private boolean isConsortiumTenant() {
    return consortiumTenant != null && consortiumTenant.equals(context.getTenantId());
  }

  private AuthorityDataStatDtoCollection fetchMemberTenantStats(int limit) {
    if (toDate() != null) {
      return entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toDate());
    }
    return null;
  }

  private AuthorityDataStatDtoCollection fetchCentralTenantStats(int limit) {
    if (toConsortiumDate() != null) {
      var centralTenantStats = executionService.execute(consortiumTenant, context, () ->
        entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toConsortiumDate()));
      if (centralTenantStats != null && !centralTenantStats.getStats().isEmpty()) {
        centralTenantStats.getStats().forEach(stat -> stat.setShared(true));
      }
      return centralTenantStats;
    }
    return null;
  }

  private AuthorityDataStatDtoCollection getDataFromOverflowStats(int limit) {
    if (overflowStats == null) {
      return getMergedAuthorityStats(null, null, null);
    }
    if (overflowStats.size() > limit) {
      var resultStats = new ArrayList<>(overflowStats.subList(0, limit));
      overflowStats.subList(0, limit).clear();
      return getMergedAuthorityStats(resultStats, null, null);
    }
    var result = getMergedAuthorityStats(overflowStats, null, null);
    overflowStats = null;
    return result;
  }

  private List<AuthorityDataStatDto> mergeStats(AuthorityDataStatDtoCollection memberTenantStats,
                                                AuthorityDataStatDtoCollection centralTenantStats) {

    List<AuthorityDataStatDto> mergedStats = new ArrayList<>();

    if (memberTenantStats != null) {
      mergedStats.addAll(memberTenantStats.getStats());
    }
    if (centralTenantStats != null) {
      mergedStats.addAll(centralTenantStats.getStats());
    }
    if (overflowStats != null && !overflowStats.isEmpty()) {
      mergedStats.addAll(overflowStats);
      overflowStats = null;
    }
    return sortStatsByStartedAtDesc(mergedStats);
  }

  private AuthorityDataStatDtoCollection createStatsPage(int limit, List<AuthorityDataStatDto> mergedStats,
                                                         AuthorityDataStatDtoCollection memberTenantStats,
                                                         AuthorityDataStatDtoCollection centralTenantStats) {
    var resultStats = mergedStats.stream().limit(limit).toList();
    overflowStats = mergedStats.subList(limit, mergedStats.size());
    return getMergedAuthorityStats(resultStats, memberTenantStats, centralTenantStats);
  }

  private AuthorityDataStatDtoCollection getMergedAuthorityStats(List<AuthorityDataStatDto> mergedStats,
                                                                 AuthorityDataStatDtoCollection memberTenantStats,
                                                                 AuthorityDataStatDtoCollection centralTenantStats) {
    return new AuthorityDataStatDtoCollection()
      .stats(mergedStats)
      .next(memberTenantStats != null ? memberTenantStats.getNext() : null)
      .consortiumNext(centralTenantStats != null ? centralTenantStats.getNext() : null);
  }

  private List<AuthorityDataStatDto> sortStatsByStartedAtDesc(List<AuthorityDataStatDto> stats) {
    return stats.stream()
      .sorted(Comparator.comparing(tenantStats -> tenantStats.getMetadata().getStartedAt()))
      .toList()
      .reversed();
  }
}
