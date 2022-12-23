package org.folio.entlinks.service.messaging.authority.handler;

import java.util.List;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeHolder;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;

public interface AuthorityChangeHandler {

  List<LinksChangeEvent> handle(List<AuthorityChangeHolder> changes);

  LinksChangeEvent.TypeEnum getReplyEventType();

  AuthorityChangeType supportedAuthorityChangeType();

}
