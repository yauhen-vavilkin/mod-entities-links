package org.folio.entlinks.service.messaging.authority.handler;

import java.util.List;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.dto.InventoryEventType;
import org.folio.entlinks.domain.dto.LinksChangeEvent;

public interface AuthorityChangeHandler {

  List<LinksChangeEvent> handle(List<InventoryEvent> events);

  LinksChangeEvent.TypeEnum getReplyEventType();

  InventoryEventType supportedInventoryEventType();

}
