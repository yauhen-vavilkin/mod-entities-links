package org.folio.entlinks.integration.kafka;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.dto.InventoryEventType;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

@Log4j2
public class AuthorityChangeFilterStrategy implements RecordFilterStrategy<String, InventoryEvent> {

  @Override
  public boolean filter(@NotNull ConsumerRecord<String, InventoryEvent> consumerRecord) {
    var inventoryEvent = consumerRecord.value();
    InventoryEventType eventType;
    try {
      eventType = InventoryEventType.valueOf(inventoryEvent.getType());
    } catch (IllegalArgumentException e) {
      log.debug("Skip message. Unsupported parameter [type: {}]", inventoryEvent.getType());
      return true;
    }

    return switch (eventType) {
      case UPDATE -> {
        if (authorityHasChanges(inventoryEvent)) {
          yield false;
        } else {
          log.debug("Skip message. No significant changes in authority record");
          yield true;
        }
      }
      case DELETE -> false;
    };
  }

  private boolean authorityHasChanges(InventoryEvent inventoryEvent) {
    return inventoryEvent.getOld() != null
      && inventoryEvent.getNew() != null
      && !inventoryEvent.getOld().equals(inventoryEvent.getNew());
  }

}
