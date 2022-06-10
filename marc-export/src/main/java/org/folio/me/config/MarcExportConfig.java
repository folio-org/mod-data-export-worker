package org.folio.me.config;

import org.folio.me.client.SearchClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = SearchClient.class)
public class MarcExportConfig {
}
