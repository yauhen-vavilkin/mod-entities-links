package org.folio.entlinks.integration.dto;

import java.util.UUID;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.service.reindex.event.DomainEvent;
import org.folio.entlinks.service.reindex.event.DomainEventType;

public class AuthorityDomainEvent extends DomainEvent<AuthorityDto> {

  public AuthorityDomainEvent(UUID id, AuthorityDto oldEntity, AuthorityDto newEntity,
                              DomainEventType type, String tenant) {
    super(id, oldEntity, newEntity, type, tenant);
  }

  public AuthorityDomainEvent(UUID id) {
    super(id, null, null, null, null);
  }
}
