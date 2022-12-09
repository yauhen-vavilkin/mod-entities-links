package org.folio.entlinks.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("folio.instance-authority.change")
public class InstanceAuthorityChangeProperties {

  private int numPartitions = 100;

}
