package org.folio.dew.batch.authoritycontrol.readers;

import static org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto.ActionEnum.UPDATE_HEADING;

import java.util.ArrayList;
import java.util.List;
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
  private final String consortiumTenant;
  private final boolean isConsortiumMemberTenant;
  private final boolean isConsortiumTenant;

  private final List<AuthorityDataStatDto> memberTenantStats = new ArrayList<>();
  private final List<AuthorityDataStatDto> consortiumTenantStats = new ArrayList<>();

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
    this.isConsortiumMemberTenant = consortiumTenant != null && !consortiumTenant.equals(this.context.getTenantId());
    this.isConsortiumTenant = consortiumTenant != null && consortiumTenant.equals(this.context.getTenantId());
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
    if (isConsortiumTenant) {
      return getAuthorityStatsFromCentralTenant(limit);
    }
    return entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toDate());
  }

  private AuthorityDataStatDto getConsortiumAuthorityDataStat(int limit) {
    if (memberTenantStats.isEmpty()) {
      loadNextMemberTenantStatsPage(limit);
    }
    if (consortiumTenantStats.isEmpty()) {
      loadNextCentralTenantStatsPage(limit);
    }
    // if there are no stats in both member and central tenant return null to finish reading
    if (memberTenantStats.isEmpty() && consortiumTenantStats.isEmpty()) {
      return null;
    }

    if (memberTenantStats.isEmpty()) {
      return consortiumTenantStats.removeFirst();
    }

    if (consortiumTenantStats.isEmpty()) {
      return memberTenantStats.removeFirst();
    }

    var memberStat = memberTenantStats.getFirst();
    var consortiumStat = consortiumTenantStats.getFirst();

    return memberStat.getMetadata().getStartedAt().isAfter(consortiumStat.getMetadata().getStartedAt())
      ? memberTenantStats.removeFirst()
      : consortiumTenantStats.removeFirst();
  }

  private AuthorityDataStatDtoCollection getAuthorityStatsFromCentralTenant(int limit) {
    var authorityStats = entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toDate());
    if (authorityStats != null && authorityStats.getStats() != null && !authorityStats.getStats().isEmpty()) {
      authorityStats.getStats().forEach(stat -> stat.setShared(true));
    }
    return authorityStats;
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
    if (toConsortiumDate() != null) {
      var authorityStats = executionService.execute(consortiumTenant, context, () ->
        entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toConsortiumDate()));

      if (authorityStats != null && !authorityStats.getStats().isEmpty()) {
        authorityStats.getStats().forEach(stat -> stat.setShared(true));
        toConsortiumDate = authorityStats.getNext();
        consortiumTenantStats.addAll(authorityStats.getStats());
      }
    }
  }
}
