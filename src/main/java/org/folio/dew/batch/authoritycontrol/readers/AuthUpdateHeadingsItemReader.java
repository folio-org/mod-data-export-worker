package org.folio.dew.batch.authoritycontrol.readers;

import static org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto.ActionEnum.UPDATE_HEADING;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.dew.client.EntitiesLinksStatsClient;
import org.folio.dew.config.properties.AuthorityControlJobProperties;
import org.folio.dew.domain.dto.authority.control.AuthorityControlExportConfig;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDtoCollection;
import org.folio.dew.service.FolioTenantService;
import org.folio.dew.service.UserTenantsService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@StepScope
@Component
public class AuthUpdateHeadingsItemReader extends AuthorityControlItemReader<AuthorityDataStatDto> {
  private final FolioTenantService folioTenantService;
  private final FolioExecutionContext context;
  private final FolioExecutionContextService executionService;
  private final UserTenantsService userTenantsService;
  private final boolean isConsortiumTenant;
  private final String consortiumId;

  private List<AuthorityDataStatDto> stats;

  public AuthUpdateHeadingsItemReader(EntitiesLinksStatsClient entitiesLinksStatsClient,
                                      AuthorityControlExportConfig exportConfig,
                                      AuthorityControlJobProperties jobProperties,
                                      FolioTenantService folioTenantService,
                                      FolioExecutionContext context,
                                      FolioExecutionContextService executionService,
                                      UserTenantsService userTenantsService) {
    super(entitiesLinksStatsClient, exportConfig, jobProperties);
    this.folioTenantService = folioTenantService;
    this.context = context;
    this.executionService = executionService;
    this.userTenantsService = userTenantsService;
    this.isConsortiumTenant = this.folioTenantService.isConsortiumTenant();
    this.consortiumId =
      isConsortiumTenant ? this.userTenantsService.getConsortiumId(context.getTenantId()).orElse(null) : null;
  }

  @Override
  protected AuthorityDataStatDtoCollection getCollection(int limit) {
    if (consortiumId != null) {
      AuthorityDataStatDtoCollection memberTenantStats = null;
      if (toDate() != null) {
        memberTenantStats = entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toDate());
      }
      AuthorityDataStatDtoCollection centralTenantStats = null;
      if (toConsortiumDate() != null) {
        var userId = Optional.ofNullable(context.getUserId())
          .map(UUID::toString)
          .orElse(null);
        Map<String, Collection<String>> headers = Map.of(XOkapiHeaders.USER_ID, List.of(userId));
        centralTenantStats = executionService.execute(consortiumId, headers, () ->
          entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toConsortiumDate()));
      }

      List<AuthorityDataStatDto> mergedStats;
      if (memberTenantStats == null && centralTenantStats == null) {
        if (stats == null) {
          return new AuthorityDataStatDtoCollection()
            .stats(null)
            .next(null)
            .consortiumNext(null);
        }
        if (stats.size() > limit) {
          mergedStats = stats.stream().limit(limit).toList();
          stats = mergedStats.subList(limit, mergedStats.size() - 1);
          return new AuthorityDataStatDtoCollection()
            .stats(mergedStats)
            .next(null)
            .consortiumNext(null);
        } else {
          var result = new AuthorityDataStatDtoCollection()
            .stats(stats)
            .next(null)
            .consortiumNext(null);
          stats = null;
          return result;
        }
      }
      if (memberTenantStats != null && centralTenantStats != null) {
        mergedStats = mergeStatsSorted(memberTenantStats.getStats(), centralTenantStats.getStats());
      } else {
        mergedStats = memberTenantStats != null ? memberTenantStats.getStats() : centralTenantStats.getStats();
      }
      if (stats != null) {
        mergedStats = Stream.concat(stats.stream(), mergedStats.stream()).toList();
        stats = null;
      }
      if (mergedStats.size() > limit) {
        var resultStats = mergedStats.stream().limit(limit).toList();
        stats = mergedStats.subList(limit, mergedStats.size() - 1);
        return new AuthorityDataStatDtoCollection()
          .stats(resultStats)
          .next(memberTenantStats != null ? memberTenantStats.getNext() : null)
          .consortiumNext(centralTenantStats != null ? centralTenantStats.getNext() : null);
      }
      return new AuthorityDataStatDtoCollection()
        .stats(mergedStats)
        .next(memberTenantStats != null ? memberTenantStats.getNext() : null)
        .consortiumNext(centralTenantStats != null ? centralTenantStats.getNext() : null);
    }
    return entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toDate());
  }

  public List<AuthorityDataStatDto> mergeStatsSorted(List<AuthorityDataStatDto> memberTenantStats,
                                                     List<AuthorityDataStatDto> centralTenantStats) {
    return Stream.concat(memberTenantStats.stream(), centralTenantStats.stream())
      .sorted(Comparator.comparing(tenantStats -> tenantStats.getMetadata().getStartedAt()))
      .toList();
  }
}
