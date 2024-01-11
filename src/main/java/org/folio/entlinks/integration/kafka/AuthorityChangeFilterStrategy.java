package org.folio.entlinks.integration.kafka;

import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.integration.dto.event.AuthorityDeleteEventSubType;
import org.folio.entlinks.integration.dto.event.AuthorityDomainEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

@Log4j2
public class AuthorityChangeFilterStrategy implements RecordFilterStrategy<String, AuthorityDomainEvent> {

  /**
   * Skipping authority domain events:
   * <ul>
   *    <li>CREATE</li>
   *    <li>REINDEX</li>
   *    <li>UPDATE (if there are no changes)</li>
   *    <li>DELETE (if it's a HARD_DELETE)</li>
   * </ul>.
   */
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
      case DELETE -> AuthorityDeleteEventSubType.HARD_DELETE.equals(authorityEvent.getDeleteEventSubType());
    };
  }

  private boolean authorityHasChanges(AuthorityDomainEvent authorityEvent) {
    return !Objects.equals(authorityEvent.getOldEntity(), authorityEvent.getNewEntity());
  }

}
