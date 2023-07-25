package org.folio.entlinks.service.reindex;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.reindex.event.DomainEvent;
import org.folio.entlinks.service.reindex.event.DomainEventType;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class AuthorityReindexJobRunner implements ReindexJobRunner {

  private static final String REINDEX_JOB_ID_HEADER = "reindex-job-id";
  private static final String DOMAIN_EVENT_TYPE_HEADER = "domain-event-type";

  private final AuthorityRepository repository;
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

  //@Transactional(readOnly = true)
  private void streamAuthorities(ReindexContext context) {
    var totalRecords = repository.count();
    log.info("reindex::count={}", totalRecords);
    ReindexJobProgressTracker progressTracker = new ReindexJobProgressTracker((int) totalRecords);

    try (var authorityStream = repository.streamAll()) {
      authorityStream
        .forEach(authority -> {
          publishEvent(authority, context);
          progressTracker.incrementProcessedCount();
          reindexService.logJobProgress(progressTracker, context.getJobId());
        });
    } catch (Exception e) {
      log.warn(e);
      reindexService.logJobFailed(context.getJobId());
      return;
    }

    // should we check progressTracker.getProcessedCount() == progressTracker.getTotalRecords() and then log success ?
    reindexService.logJobSuccess(context.getJobId());
  }

  private void publishEvent(Authority authority, ReindexContext context) {
    var id = authority.getId();
    if (id == null) {
      log.warn("Persisted Authority cannot have null id: {}", authority);
      return;
    }

    log.info("reindex::process authority id={}", id);
    var domainEvent = DomainEvent.reindexEvent(context.getTenantId(), authority);
    eventProducer.sendMessage(id.toString(), domainEvent,
        REINDEX_JOB_ID_HEADER, context.getJobId(), DOMAIN_EVENT_TYPE_HEADER, DomainEventType.REINDEX);
  }
}
