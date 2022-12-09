package org.folio.entlinks.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "org.folio.entlinks.client")
public class FeignClientsConfiguration {
}
