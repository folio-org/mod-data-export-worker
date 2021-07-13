package org.folio.dew.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application properties for Data Source.
 */
@Data
@Component
@ConfigurationProperties("application.datasource")
public class DataSourceProperties {

  /**
   * Class name for {@link java.sql.Driver}
   */
  private String driver;

  /**
   * The database url of the form jdbc:subprotocol:subname.
   */
  private String jdbcUrl;

  /**
   * The username to use for DataSource.getConnection(username, password) calls.
   */
  private String username;

  /**
   * The password to use for DataSource.getConnection(username, password) calls.
   */
  private String password;

  /**
   * The property controls the maximum size that the pool is allowed to reach.
   */
  private int maxPoolSize;
}
