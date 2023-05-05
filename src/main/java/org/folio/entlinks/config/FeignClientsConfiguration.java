package org.folio.entlinks.config;

import org.folio.processing.mapping.defaultmapper.MarcToAuthorityMapper;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "org.folio.entlinks.client")
public class FeignClientsConfiguration {

  @Bean
  public MarcToAuthorityMapper mapper() {
    return new MarcToAuthorityMapper();
  }
}
