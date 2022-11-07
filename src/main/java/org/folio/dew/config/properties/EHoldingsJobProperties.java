package org.folio.dew.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application properties for eHoldings batch job configuration
 */
@Data
@Component
@ConfigurationProperties("application.e-holdings-batch")
public class EHoldingsJobProperties {

  /**
   * Spring batch job chunk size.
   */
  private int chunkSize;
}
