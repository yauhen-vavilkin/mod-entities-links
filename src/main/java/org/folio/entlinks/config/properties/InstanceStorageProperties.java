package org.folio.entlinks.config.properties;

import jakarta.validation.constraints.Max;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties("folio.instance-storage")
public class InstanceStorageProperties {

  /**
   * Provides batch size for inventory-storage.
   * Max 90 - based on maximum URI length
   */
  @Max(90)
  private int batchSize = 50;
}
