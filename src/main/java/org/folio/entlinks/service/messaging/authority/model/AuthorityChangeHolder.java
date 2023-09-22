package org.folio.entlinks.service.messaging.authority.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.folio.entlinks.integration.dto.AuthorityDomainEvent;
import org.folio.entlinks.service.reindex.event.DomainEventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class AuthorityChangeHolder {

  @Getter
  private final @NotNull AuthorityDomainEvent event;
  private final @NotNull Map<AuthorityChangeField, AuthorityChange> changes;
  private final @NotNull Map<AuthorityChangeField, String> fieldTagRelation;
  @Getter
  private final int numberOfLinks;

  @Getter
  @Setter
  private UUID authorityDataStatId;

  public UUID getAuthorityId() {
    return event.getId();
  }

  public String getNewNaturalId() {
    return getNaturalId(event.getNewEntity());
  }

  public String getOldNaturalId() {
    return getNaturalId(event.getOldEntity());
  }

  public UUID getNewSourceFileId() {
    return getSourceFileId(event.getNewEntity());
  }

  public UUID getOldSourceFileId() {
    return getSourceFileId(event.getOldEntity());
  }

  public boolean isNaturalIdChanged() {
    return changes.containsKey(AuthorityChangeField.NATURAL_ID);
  }

  public boolean isOnlyNaturalIdChanged() {
    return changes.size() == 1 && isNaturalIdChanged();
  }

  public AuthorityChangeType getChangeType() {
    return switch (event.getType()) {
      case UPDATE -> isHeadingTypeChanged() ? AuthorityChangeType.DELETE : AuthorityChangeType.UPDATE;
      case DELETE -> AuthorityChangeType.DELETE;
      case CREATE, REINDEX -> null;
    };
  }

  public AuthorityChangeField getFieldChange() {
    if (changes.isEmpty() || isOnlyNaturalIdChanged()) {
      return null;
    } else {
      var authorityChanges = new ArrayList<>(changes.keySet());
      authorityChanges.remove(AuthorityChangeField.NATURAL_ID);
      return authorityChanges.get(0);
    }
  }

  public boolean changesExist() {
    return MapUtils.isNotEmpty(changes);
  }

  public AuthorityDataStat toAuthorityDataStat() {
    var changeMap = new EnumMap<>(changes);
    changeMap.remove(AuthorityChangeField.NATURAL_ID);

    String headingNew = null;
    String headingOld = null;
    String headingTypeNew = null;
    String headingTypeOld = null;

    if (changeMap.size() == 1) {
      var changeEntry = changeMap.entrySet().iterator().next();
      var entryValue = changeEntry.getValue();
      headingNew = entryValue.valNew() != null ? entryValue.valNew().toString() : null;
      headingOld = entryValue.valOld() != null ? entryValue.valOld().toString() : null;
      var headingType = getHeadingType(changeEntry);
      headingTypeNew = headingType;
      headingTypeOld = headingType;
    } else {
      for (var changeEntry : changeMap.entrySet()) {
        if (changeEntry.getValue().valNew() != null) {
          headingNew = changeEntry.getValue().valNew().toString();
          headingTypeNew = getHeadingType(changeEntry);
        } else if (changeEntry.getValue().valOld() != null) {
          headingOld = changeEntry.getValue().valOld().toString();
          headingTypeOld = getHeadingType(changeEntry);
        }
      }
    }

    var authority = new Authority();
    authority.setId(getAuthorityId());
    authority.setNaturalId(getNewNaturalId() == null ? getOldNaturalId() : getNewNaturalId());
    authority.setDeleted(event.getType().equals(DomainEventType.DELETE));
    AuthorityDataStat authorityDataStat = AuthorityDataStat.builder()
        .authority(authority)
        .authorityNaturalIdOld(getOldNaturalId())
        .authorityNaturalIdNew(getNewNaturalId())
        .authoritySourceFileOld(getOldSourceFileId())
        .authoritySourceFileNew(getNewSourceFileId())
        .headingOld(headingOld)
        .headingNew(headingNew)
        .headingTypeOld(headingTypeOld)
        .headingTypeNew(headingTypeNew)
        .action(getAuthorityDataStatAction())
        .lbTotal(numberOfLinks)
        .build();
    if (this.event.getNewEntity() != null && this.event.getNewEntity().getMetadata() != null) {
      authorityDataStat.setStartedByUserId(this.event.getNewEntity().getMetadata().getUpdatedByUserId());
    }

    return authorityDataStat;
  }

  private AuthorityDataStatAction getAuthorityDataStatAction() {
    return switch (event.getType()) {
      case UPDATE -> isOnlyNaturalIdChanged()
                     ? AuthorityDataStatAction.UPDATE_NATURAL_ID
                     : AuthorityDataStatAction.UPDATE_HEADING;
      case DELETE -> AuthorityDataStatAction.DELETE;
      case CREATE, REINDEX -> null;
    };
  }

  private String getHeadingType(Map.Entry<AuthorityChangeField, AuthorityChange> changeEntry) {
    return fieldTagRelation.get(changeEntry.getValue().changeField());
  }

  @Nullable
  private String getNaturalId(AuthorityDto inventoryRecord) {
    return inventoryRecord != null ? inventoryRecord.getNaturalId() : null;
  }

  @Nullable
  private UUID getSourceFileId(AuthorityDto inventoryRecord) {
    return inventoryRecord != null ? inventoryRecord.getSourceFileId() : null;
  }

  private boolean isHeadingTypeChanged() {
    return changes.size() > 2 || changes.size() == 2 && !changes.containsKey(AuthorityChangeField.NATURAL_ID);
  }

}
