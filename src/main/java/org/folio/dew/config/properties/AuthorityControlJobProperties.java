package org.folio.dew.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Application properties for authority control batch job configuration
 */
@Data
@Validated
@Component
@ConfigurationProperties("application.authority-control-batch")
public class AuthorityControlJobProperties {

  /**
   * Spring batch job chunk size.
   */
  @Min(1)
  private int jobChunkSize;
  /**
   * Chunk size for entities links requests.
   */
  @Min(1)
  @Max(100)
  private int entitiesLinksChunkSize;
}
