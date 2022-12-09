package org.folio.entlinks.service.messaging.authority.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.jetbrains.annotations.NotNull;

public class AuthorityChangeHolder {

  private final @NotNull InventoryEvent event;

  private final @Getter boolean isNaturalIdChanged;
  private final @Getter boolean isOnlyNaturalIdChanged;
  private final @Getter AuthorityChange fieldChange;

  public AuthorityChangeHolder(@NotNull InventoryEvent event,
                               @NotNull List<AuthorityChange> changes) {
    if (changes.isEmpty()) {
      throw new IllegalArgumentException("Changes couldn't be empty");
    }
    this.event = event;
    this.isNaturalIdChanged = changes.contains(AuthorityChange.NATURAL_ID);
    this.isOnlyNaturalIdChanged = isOnlyNaturalIdChanged(changes);
    this.fieldChange = getFieldChange(changes, isOnlyNaturalIdChanged);
  }

  public UUID getAuthorityId() {
    return event.getId();
  }

  public String getNewNaturalId() {
    return event.getNew().getNaturalId();
  }

  public UUID getNewSourceFileId() {
    return event.getNew().getSourceFileId();
  }

  private AuthorityChange getFieldChange(List<AuthorityChange> changes, boolean isOnlyNaturalIdChanged) {
    if (isOnlyNaturalIdChanged) {
      return null;
    } else {
      var authorityChanges = new ArrayList<>(changes);
      authorityChanges.remove(AuthorityChange.NATURAL_ID);
      return authorityChanges.get(0);
    }
  }

  private boolean isOnlyNaturalIdChanged(@NotNull List<AuthorityChange> changes) {
    return isNaturalIdChanged && changes.size() == 1;
  }
}
