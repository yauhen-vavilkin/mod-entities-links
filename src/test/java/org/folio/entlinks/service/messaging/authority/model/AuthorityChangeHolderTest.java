package org.folio.entlinks.service.messaging.authority.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.junit.jupiter.api.Test;

class AuthorityChangeHolderTest {

  @Test
  void construct_negative() {
    var event = new InventoryEvent();
    var changes = Collections.<AuthorityChange>emptyList();
    assertThrows(IllegalArgumentException.class,
      () -> new AuthorityChangeHolder(event, changes));
  }

  @Test
  void getAuthorityId_positive() {
    var id = UUID.randomUUID();
    var holder = new AuthorityChangeHolder(
      new InventoryEvent().id(id),
      List.of(AuthorityChange.NATURAL_ID));

    var actual = holder.getAuthorityId();

    assertEquals(id, actual);
  }

  @Test
  void getNewNaturalId_positive() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord().naturalId("naturalNew"))
        .old(new AuthorityInventoryRecord().naturalId("naturalOld")),
      List.of(AuthorityChange.NATURAL_ID));

    var actual = holder.getNewNaturalId();

    assertEquals("naturalNew", actual);
  }

  @Test
  void getNewSourceFileId_positive() {
    var sourceFileIdNew = UUID.randomUUID();
    var sourceFileIdOld = UUID.randomUUID();
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord().sourceFileId(sourceFileIdNew))
        .old(new AuthorityInventoryRecord().sourceFileId(sourceFileIdOld)),
      List.of(AuthorityChange.NATURAL_ID));

    var actual = holder.getNewSourceFileId();

    assertEquals(sourceFileIdNew, actual);
  }

  @Test
  void isNaturalIdChanged_positive_naturalIdIsInChanges() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      List.of(AuthorityChange.PERSONAL_NAME, AuthorityChange.NATURAL_ID));

    var actual = holder.isNaturalIdChanged();

    assertTrue(actual);
  }

  @Test
  void isNaturalIdChanged_positive_naturalIdIsNotInChanges() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      List.of(AuthorityChange.PERSONAL_NAME, AuthorityChange.PERSONAL_NAME_TITLE));

    var actual = holder.isNaturalIdChanged();

    assertFalse(actual);
  }

  @Test
  void isOnlyNaturalIdChanged_positive_onlyNaturalIdIsInChanges() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      List.of(AuthorityChange.NATURAL_ID));

    var actual = holder.isOnlyNaturalIdChanged();

    assertTrue(actual);
  }

  @Test
  void isOnlyNaturalIdChanged_positive_notOnlyNaturalIdIsInChanges() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      List.of(AuthorityChange.PERSONAL_NAME, AuthorityChange.NATURAL_ID));

    var actual = holder.isOnlyNaturalIdChanged();

    assertFalse(actual);
  }

  @Test
  void isOnlyNaturalIdChanged_positive_naturalIdIsNotInChanges() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      List.of(AuthorityChange.PERSONAL_NAME, AuthorityChange.PERSONAL_NAME_TITLE));

    var actual = holder.isOnlyNaturalIdChanged();

    assertFalse(actual);
  }

  @Test
  void getFieldChange_positive_onlyFieldChange() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      List.of(AuthorityChange.PERSONAL_NAME));

    var actual = holder.getFieldChange();

    assertEquals(AuthorityChange.PERSONAL_NAME, actual);
  }

  @Test
  void getFieldChange_positive_fieldAndNaturalIdChanges() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      List.of(AuthorityChange.NATURAL_ID, AuthorityChange.PERSONAL_NAME));

    var actual = holder.getFieldChange();

    assertEquals(AuthorityChange.PERSONAL_NAME, actual);
  }
}
