package org.folio.entlinks.service.messaging.authority.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.dto.InventoryEventType;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class AuthorityChangeHolder {

  @Getter
  private final @NotNull InventoryEvent event;
  private final @NotNull List<AuthorityChange> changes;

  public UUID getAuthorityId() {
    return event.getId();
  }

  public String getNewNaturalId() {
    return event.getNew().getNaturalId();
  }

  public UUID getNewSourceFileId() {
    return event.getNew().getSourceFileId();
  }

  public boolean isNaturalIdChanged() {
    return changes.contains(AuthorityChange.NATURAL_ID);
  }

  public boolean isOnlyNaturalIdChanged() {
    return changes.size() == 1 && isNaturalIdChanged();
  }

  public AuthorityChangeType getChangeType() {
    var eventType = InventoryEventType.fromValue(event.getType());
    return switch (eventType) {
      case UPDATE -> fieldChangedUnexpected() ? AuthorityChangeType.DELETE : AuthorityChangeType.UPDATE;
      case DELETE -> AuthorityChangeType.DELETE;
    };
  }

  public AuthorityChange getFieldChange() {
    if (changes.isEmpty() || isOnlyNaturalIdChanged()) {
      return null;
    } else {
      var authorityChanges = new ArrayList<>(changes);
      authorityChanges.remove(AuthorityChange.NATURAL_ID);
      return authorityChanges.get(0);
    }
  }

  private boolean fieldChangedUnexpected() {
    return changes.size() > 2 || changes.size() == 2 && !changes.contains(AuthorityChange.NATURAL_ID);
  }

}
