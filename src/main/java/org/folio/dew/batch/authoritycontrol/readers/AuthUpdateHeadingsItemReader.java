package org.folio.dew.batch.authoritycontrol.readers;

import static org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto.ActionEnum.UPDATE_HEADING;

import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
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

@StepScope
@Component
public class AuthUpdateHeadingsItemReader extends AuthorityControlItemReader<AuthorityDataStatDto> {

  private final FolioExecutionContext context;
  private final FolioExecutionContextService executionService;
  private final String consortiumCentralTenant;
  private final boolean isConsortiumMemberTenant;
  private final boolean isConsortiumCentralTenant;
  private OffsetDateTime toConsortiumDate;

  private final Deque<AuthorityDataStatDto> memberTenantStats = new ArrayDeque<>();
  private final Deque<AuthorityDataStatDto> centralTenantStats = new ArrayDeque<>();

  public AuthUpdateHeadingsItemReader(EntitiesLinksStatsClient entitiesLinksStatsClient,
                                      AuthorityControlExportConfig exportConfig,
                                      AuthorityControlJobProperties jobProperties,
                                      FolioTenantService folioTenantService,
                                      FolioExecutionContext context,
                                      FolioExecutionContextService executionService) {
    super(entitiesLinksStatsClient, exportConfig, jobProperties);
    this.context = context;
    this.executionService = executionService;
    this.consortiumCentralTenant = folioTenantService.getConsortiumCentralTenant();
    this.isConsortiumMemberTenant = consortiumCentralTenant != null && !consortiumCentralTenant.equals(this.context.getTenantId());
    this.isConsortiumCentralTenant = consortiumCentralTenant != null && consortiumCentralTenant.equals(this.context.getTenantId());
    this.toConsortiumDate = this.toDate;
  }

  @Override
  protected AuthorityDataStatDto doRead() {
    if (!isConsortiumMemberTenant) {
      return super.doRead();
    }
    return getConsortiumAuthorityDataStat(limit);
  }

  @Override
  protected AuthorityDataStatDtoCollection getCollection(int limit) {
    var authorityStats = entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toDate());
    if (isConsortiumCentralTenant && authorityStats != null && !authorityStats.getStats().isEmpty()) {
      authorityStats.getStats().forEach(stat -> stat.setShared(true));
    }
    return authorityStats;
  }

  private AuthorityDataStatDto getConsortiumAuthorityDataStat(int limit) {
    if (memberTenantStats.isEmpty()) {
      loadNextMemberTenantStatsPage(limit);
    }
    if (centralTenantStats.isEmpty()) {
      loadNextCentralTenantStatsPage(limit);
    }
    // if there are no stats in both member and central tenant return null to finish reading
    if (memberTenantStats.isEmpty() && centralTenantStats.isEmpty()) {
      return null;
    }

    if (memberTenantStats.isEmpty()) {
      return centralTenantStats.pollFirst();
    }
    if (centralTenantStats.isEmpty()) {
      return memberTenantStats.pollFirst();
    }

    var memberStat = memberTenantStats.peekFirst();
    var centralStat = centralTenantStats.peekFirst();

    return memberStat.getMetadata().getStartedAt().isAfter(centralStat.getMetadata().getStartedAt())
      ? memberTenantStats.pollFirst()
      : centralTenantStats.pollFirst();
  }

  private void loadNextMemberTenantStatsPage(int limit) {
    if (toDate() != null) {
      var authorityStats = entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toDate());
      if (authorityStats != null) {
        toDate = authorityStats.getNext();
        memberTenantStats.addAll(authorityStats.getStats());
      }
    }
  }

  private void loadNextCentralTenantStatsPage(int limit) {
    if (formatConsortiumDate() != null) {
      var authorityStats = executionService.execute(consortiumCentralTenant, context, () ->
        entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), formatConsortiumDate()));

      if (authorityStats != null && !authorityStats.getStats().isEmpty()) {
        authorityStats.getStats().forEach(stat -> stat.setShared(true));
        toConsortiumDate = authorityStats.getNext();
        centralTenantStats.addAll(authorityStats.getStats());
      }
    }
  }

  private String formatConsortiumDate() {
    return toConsortiumDate == null ? null : toConsortiumDate.toString();
  }
}
