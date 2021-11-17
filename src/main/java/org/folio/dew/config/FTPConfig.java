package org.folio.dew.config;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dew.config.properties.FTPProperties;
import org.folio.dew.repository.FTPObjectStorageRepository;
import org.folio.dew.utils.FTPCommandLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FTPConfig {

  private static final Logger logger = LogManager.getLogger(FTPObjectStorageRepository.class);

  @Bean(name = "FTPClientConfig")
  public FTPClient getFTPClientConfig(FTPProperties properties) {
    FTPClient ftp = new FTPClient();
    ftp.setDefaultTimeout(properties.getDefaultTimeout());
    ftp.addProtocolCommandListener(FTPCommandLogger.getDefListener(logger));
    ftp.setControlKeepAliveTimeout(properties.getControlKeepAliveTimeout());
    ftp.setBufferSize(properties.getBufferSize());
    ftp.setPassiveNatWorkaroundStrategy(new FTPConfig.DefaultServerResolver(ftp));

    return ftp;
  }

  public static class DefaultServerResolver implements FTPClient.HostnameResolver {
    private FTPClient client;

    public DefaultServerResolver(FTPClient client) {
      this.client = client;
    }

    @Override
    public String resolve(String hostname) {
      return this.client.getRemoteAddress().getHostAddress();
    }
  }
}
