package org.folio.dew.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.AccountBulkClient;
import org.folio.dew.client.AccountClient;
import org.folio.dew.client.AgreementClient;
import org.folio.dew.client.AuditClient;
import org.folio.dew.client.DataExportSpringClient;
import org.folio.dew.client.EntitiesLinksStatsClient;
import org.folio.dew.client.ExpenseClassClient;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.client.IdentifierTypeClient;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.client.LocaleClient;
import org.folio.dew.client.LocationClient;
import org.folio.dew.client.MaterialTypeClient;
import org.folio.dew.client.NotesClient;
import org.folio.dew.client.OrdersStorageClient;
import org.folio.dew.client.OrganizationsClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.client.TenantAddressesClient;
import org.folio.dew.client.TransferClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.client.UserTenantsClient;
import org.folio.dew.error.RestClientErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@Log4j2
@RequiredArgsConstructor
public class HttpClientConfiguration {

  private final RestClientErrorHandler errorHandler;

  @Bean
  public NotesClient notesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(NotesClient.class);
  }

  @Bean
  public UserTenantsClient userTenantsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserTenantsClient.class);
  }

  @Bean
  public TransferClient transferClient(HttpServiceProxyFactory factory) {
    return factory.createClient(TransferClient.class);
  }

  @Bean
  public UserClient userClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserClient.class);
  }

  @Bean
  public TenantAddressesClient tenantAddressesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(TenantAddressesClient.class);
  }

  @Bean
  public AccountClient accountClient(HttpServiceProxyFactory factory) {
    return factory.createClient(AccountClient.class);
  }

  @Bean
  public AccountBulkClient accountBulkClient(HttpServiceProxyFactory factory) {
    return factory.createClient(AccountBulkClient.class);
  }

  @Bean
  public AgreementClient agreementClient(HttpServiceProxyFactory factory) {
    return factory.createClient(AgreementClient.class);
  }

  @Bean
  public AuditClient auditClient(HttpServiceProxyFactory factory) {
    return factory.createClient(AuditClient.class);
  }

  @Bean
  public DataExportSpringClient dataExportSpringClient(HttpServiceProxyFactory factory) {
    return factory.createClient(DataExportSpringClient.class);
  }

  @Bean
  public EntitiesLinksStatsClient entitiesLinksStatsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(EntitiesLinksStatsClient.class);
  }

  @Bean
  public ExpenseClassClient expenseClassClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ExpenseClassClient.class);
  }

  @Bean
  public HoldingClient holdingClient(HttpServiceProxyFactory factory) {
    return factory.createClient(HoldingClient.class);
  }

  @Bean
  public IdentifierTypeClient identifierTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(IdentifierTypeClient.class);
  }

  @Bean
  public InventoryClient inventoryClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InventoryClient.class);
  }

  @Bean
  public KbEbscoClient kbEbscoClient(HttpServiceProxyFactory factory) {
    return factory.createClient(KbEbscoClient.class);
  }

  @Bean
  public LocaleClient localeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LocaleClient.class);
  }

  @Bean
  public LocationClient locationClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LocationClient.class);
  }

  @Bean
  public MaterialTypeClient materialTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(MaterialTypeClient.class);
  }

  @Bean
  public OrdersStorageClient ordersStorageClient(HttpServiceProxyFactory factory) {
    return factory.createClient(OrdersStorageClient.class);
  }

  @Bean
  public OrganizationsClient organizationsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(OrganizationsClient.class);
  }

  @Bean
  public ServicePointClient servicePointClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ServicePointClient.class);
  }

  @Primary
  @Bean
  public RestClient restClient(RestClient.Builder builder) {
    return builder
        .requestInterceptor(
            (request, body, execution) -> {
              log.debug("Request URL: {}", request.getURI());
              request.getHeaders().add(HttpHeaders.ACCEPT_ENCODING, "identity");
              return execution.execute(request, body);
            })
        .defaultStatusHandler(HttpStatusCode::isError, errorHandler::handle)
        .build();
  }

  @Bean
  @Primary
  public HttpServiceProxyFactory factory(
          RestClient.Builder builder,
          FormattingConversionService formattingConversionService) {

    formattingConversionService.addConverter(new NotesClient.NoteLinkTypeToPathVarConverter());
    formattingConversionService.addConverter(new NotesClient.NoteLinkDomainToPathVarConverter());

    RestClient restClient = builder.build();
    RestClientAdapter adapter = RestClientAdapter.create(restClient);

    return HttpServiceProxyFactory.builderFor(adapter)
            .conversionService(formattingConversionService)
//            .customArgumentResolver(new NullableRequestParamArgumentResolver())
            .build();
  }

}
