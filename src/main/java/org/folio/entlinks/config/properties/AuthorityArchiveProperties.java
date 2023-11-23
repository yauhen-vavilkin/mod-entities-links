package org.folio.entlinks.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "folio.authority-archive.expire")
public class AuthorityArchiveProperties {

  @Min(1)
  private int retentionPeriodInDays;

}
