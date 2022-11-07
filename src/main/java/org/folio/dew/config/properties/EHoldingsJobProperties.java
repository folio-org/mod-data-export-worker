package org.folio.dew.config.properties;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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
  @Min(1)
  private int jobChunkSize;
  /**
   * Chunk size for kb ebsco requests.
   */
  @Min(1)
  @Max(100)
  private int kbEbscoChunkSize;
}
