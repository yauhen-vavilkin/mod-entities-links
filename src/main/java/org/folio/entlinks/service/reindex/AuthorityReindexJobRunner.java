package org.folio.entlinks.service.reindex;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.reindex.event.DomainEvent;
import org.folio.entlinks.service.reindex.event.DomainEventType;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class AuthorityReindexJobRunner implements ReindexJobRunner {

  private static final String COUNT_QUERY = "SELECT COUNT(*) FROM %s_mod_inventory_storage.authority";
  private static final String SELECT_QUERY = "SELECT * FROM %s_mod_inventory_storage.authority";

  private static final String REINDEX_JOB_ID_HEADER = "reindex-job-id";
  private static final String DOMAIN_EVENT_TYPE_HEADER = "domain-event-type";

  private final JdbcTemplate jdbcTemplate;
  private final EventProducer<DomainEvent<?>> eventProducer;
  private final FolioExecutionContext folioExecutionContext;
  private final ReindexService reindexService;

  @Async
  @Override
  public void startReindex(ReindexJob reindexJob) {
    log.info("reindex::started");
    var reindexContext = new ReindexContext(reindexJob, folioExecutionContext);
    streamAuthorities(reindexContext);
    log.info("reindex::ended");
  }

  private void streamAuthorities(ReindexContext context) {
    var totalRecords = jdbcTemplate.queryForObject(String.format(COUNT_QUERY, context.getTenantId()), Integer.class);
    log.info("reindex::count={}", totalRecords);
    ReindexJobProgressTracker progressTracker = new ReindexJobProgressTracker(totalRecords == null ? 0 : totalRecords);

    try (var authorityStream = jdbcTemplate.queryForStream(String.format(SELECT_QUERY, context.getTenantId()),
      (rs, rowNum) -> toAuthority(rs))) {

      authorityStream
        .forEach(authority -> {
          var id = authority.getId();
          log.info("reindex::process authority id={}", id);
          var domainEvent = DomainEvent.reindexEvent(context.getTenantId(), authority);
          eventProducer.sendMessage(id.toString(), domainEvent,
            REINDEX_JOB_ID_HEADER, context.getJobId(), DOMAIN_EVENT_TYPE_HEADER, DomainEventType.REINDEX);
          progressTracker.incrementProcessedCount();
          reindexService.logJobProgress(progressTracker, context);
        });
    } catch (Exception e) {
      log.warn(e);
      reindexService.logJobFailed(context);
    } finally {
      reindexService.logJobSuccess(context);
    }
  }

  private Authority toAuthority(ResultSet rs) {
    try {
      var jsonb = rs.getString("jsonb");
      var objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      return objectMapper.readValue(jsonb, Authority.class);
    } catch (SQLException | JsonProcessingException e) {
      log.warn(e);
      throw new RuntimeException(e);
    }
  }

}
