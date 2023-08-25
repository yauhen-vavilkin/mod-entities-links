package org.folio.entlinks.integration.kafka;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.domain.dto.AuthorityEvent;
import org.folio.entlinks.domain.dto.AuthorityEventType;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

@Log4j2
public class AuthorityChangeFilterStrategy implements RecordFilterStrategy<String, AuthorityEvent> {

  @Override
  public boolean filter(@NotNull ConsumerRecord<String, AuthorityEvent> consumerRecord) {
    var authorityEvent = consumerRecord.value();
    AuthorityEventType eventType;
    try {
      eventType = AuthorityEventType.valueOf(authorityEvent.getType());
    } catch (IllegalArgumentException e) {
      log.debug("Skip message. Unsupported parameter [type: {}]", authorityEvent.getType());
      return true;
    }

    return switch (eventType) {
      case UPDATE -> {
        if (authorityHasChanges(authorityEvent)) {
          yield false;
        } else {
          log.debug("Skip message. No significant changes in authority record");
          yield true;
        }
      }
      case DELETE -> false;
    };
  }

  private boolean authorityHasChanges(AuthorityEvent authorityEvent) {
    return authorityEvent.getOld() != null && authorityEvent.getNew() != null
        && !authorityEvent.getOld().equals(authorityEvent.getNew());
  }

}
