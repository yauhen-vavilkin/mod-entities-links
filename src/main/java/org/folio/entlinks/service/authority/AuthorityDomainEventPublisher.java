package org.folio.entlinks.service.authority;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.dto.event.AuthorityDomainEvent;
import org.folio.entlinks.integration.dto.event.DomainEvent;
import org.folio.entlinks.integration.dto.event.DomainEventType;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.reindex.ReindexContext;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class AuthorityDomainEventPublisher {

  private static final String DOMAIN_EVENT_TYPE_HEADER = "domain-event-type";
  private static final String REINDEX_JOB_ID_HEADER = "reindex-job-id";

  private final EventProducer<DomainEvent<?>> eventProducer;
  private final FolioExecutionContext folioExecutionContext;

  public void publishCreateEvent(AuthorityDto created) {
    var id = created.getId();
    if (id == null) {
      log.warn("Created Authority cannot have null id: {}", created);
      return;
    }

    log.debug("publishCreated::process authority id={}", id);

    var domainEvent = DomainEvent.createEvent(id, created, folioExecutionContext.getTenantId());
    eventProducer.sendMessage(id.toString(), domainEvent, DOMAIN_EVENT_TYPE_HEADER, DomainEventType.CREATE);
  }

  public void publishUpdateEvent(AuthorityDto oldAuthority, AuthorityDto updatedAuthority) {
    var id = updatedAuthority.getId();
    if (id == null || oldAuthority.getId() == null) {
      log.warn("Old/New Authority cannot have null id: updated.id - {}, old.id: {}", id, oldAuthority.getId());
      return;
    }

    log.debug("publishUpdated::process authority id={}", id);

    var domainEvent = DomainEvent.updateEvent(id, oldAuthority, updatedAuthority,
      folioExecutionContext.getTenantId());
    eventProducer.sendMessage(id.toString(), domainEvent, DOMAIN_EVENT_TYPE_HEADER, DomainEventType.UPDATE);
  }

  public void publishSoftDeleteEvent(AuthorityDto deleted) {
    var id = deleted.getId();
    if (id == null) {
      log.warn("Deleted Authority cannot have null id: {}", deleted);
      return;
    }

    log.debug("publishRemoved::process authority id={}", id);

    var domainEvent = AuthorityDomainEvent.softDeleteEvent(id, deleted, folioExecutionContext.getTenantId());
    eventProducer.sendMessage(id.toString(), domainEvent, DOMAIN_EVENT_TYPE_HEADER, DomainEventType.DELETE);
  }

  public void publishHardDeleteEvent(AuthorityDto deleted) {
    var id = deleted.getId();
    if (id == null) {
      log.warn("Deleted Authority cannot have null id: {}", deleted);
      return;
    }

    log.debug("publishRemoved::process authority id={}", id);

    var domainEvent = AuthorityDomainEvent.hardDeleteEvent(id, deleted, folioExecutionContext.getTenantId());
    eventProducer.sendMessage(id.toString(), domainEvent, DOMAIN_EVENT_TYPE_HEADER, DomainEventType.DELETE);
  }

  public void publishReindexEvent(AuthorityDto authority, ReindexContext context) {
    var id = authority.getId();
    if (id == null) {
      log.warn("Persisted Authority cannot have null id: {}", authority);
      return;
    }

    log.debug("reindex::process authority id={}", id);
    var domainEvent = DomainEvent.reindexEvent(id, authority, context.getTenantId());
    eventProducer.sendMessage(id.toString(), domainEvent,
      REINDEX_JOB_ID_HEADER, context.getJobId(), DOMAIN_EVENT_TYPE_HEADER, DomainEventType.REINDEX);
  }
}
