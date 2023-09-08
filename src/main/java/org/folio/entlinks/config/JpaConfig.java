package org.folio.entlinks.config;

import java.util.Optional;
import java.util.UUID;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableRetry
public class JpaConfig {

  @Bean
  public AuditorAware<UUID> auditorProvider(FolioExecutionContext folioExecutionContext) {
    return () -> Optional.ofNullable(folioExecutionContext.getUserId());
  }
}
