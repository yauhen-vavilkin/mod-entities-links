package org.folio.entlinks.integration.kafka;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.integration.dto.AuthorityDomainEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

@Log4j2
public class AuthorityChangeFilterStrategy implements RecordFilterStrategy<String, AuthorityDomainEvent> {

  @Override
  public boolean filter(@NotNull ConsumerRecord<String, AuthorityDomainEvent> consumerRecord) {
    var authorityEvent = consumerRecord.value();

    return switch (authorityEvent.getType()) {
      case UPDATE -> {
        if (authorityHasChanges(authorityEvent)) {
          yield false;
        } else {
          log.debug("Skip message. No significant changes in authority record");
          yield true;
        }
      }
      case CREATE, REINDEX -> true;
      case DELETE -> false;
    };
  }

  private boolean authorityHasChanges(AuthorityDomainEvent authorityEvent) {
    return authorityEvent.getOldEntity() != null && authorityEvent.getNewEntity() != null
      && !authorityEvent.getOldEntity().equals(authorityEvent.getNewEntity());
  }

}
