package org.folio.entlinks.config;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;

@Log4j2
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableRetry
public class JpaConfig {

  private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  @Bean
  public AuditorAware<UUID> auditorProvider(FolioExecutionContext folioExecutionContext) {
    return () -> Optional.ofNullable(folioExecutionContext.getUserId()).or(useDefault());
  }

  private Supplier<Optional<UUID>> useDefault() {
    return () -> {
      log.warn("Current auditor cannot be determined from execution context. userId is NULL");

      return Optional.of(DEFAULT_USER_ID);
    };
  }
}
