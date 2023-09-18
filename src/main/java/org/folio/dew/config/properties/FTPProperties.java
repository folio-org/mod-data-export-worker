package org.folio.dew.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("application.ftp")
public class FTPProperties {

  /**
   * Buffer size.
   */
  private int bufferSize;

  /**
   * Default ftp port.
   */
  private int defaultPort;

  /**
   * The timeout in milliseconds to use for the socket
   * connection.
   */
  private int defaultTimeout;

  /**
   * Set the time to wait between sending control connection keepalive messages
   * when processing file upload or download.
   */
  private long controlKeepAliveTimeout;
}
