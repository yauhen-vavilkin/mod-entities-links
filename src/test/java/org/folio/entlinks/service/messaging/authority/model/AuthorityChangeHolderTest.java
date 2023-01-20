package org.folio.entlinks.service.messaging.authority.model;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField.CORPORATE_NAME;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField.NATURAL_ID;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField.PERSONAL_NAME;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField.PERSONAL_NAME_TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.dto.InventoryEventType;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.junit.jupiter.api.Test;

class AuthorityChangeHolderTest {

  public static final String[] STAT_OBJ_PROPERTIES =
    {"action", "headingOld", "headingNew", "headingTypeOld", "headingTypeNew",
     "authorityNaturalIdOld", "authorityNaturalIdNew", "authoritySourceFileOld", "authoritySourceFileNew",
     "lbTotal"};

  @Test
  void getNewNaturalId_positive() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord().naturalId("n"))
        .old(new AuthorityInventoryRecord().naturalId("o")),
      Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o")),
      Map.of(), 1);

    var actual = holder.getNewNaturalId();

    assertEquals("n", actual);
  }

  @Test
  void getNewSourceFileId_positive() {
    var sourceFileIdNew = UUID.randomUUID();
    var sourceFileIdOld = UUID.randomUUID();
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord().sourceFileId(sourceFileIdNew))
        .old(new AuthorityInventoryRecord().sourceFileId(sourceFileIdOld)),
      Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o")),
      Map.of(), 1);

    var actual = holder.getNewSourceFileId();

    assertEquals(sourceFileIdNew, actual);
  }

  @Test
  void isNaturalIdChanged_positive_naturalIdIsInChanges() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o"),
        PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.isNaturalIdChanged();

    assertTrue(actual);
  }

  @Test
  void isNaturalIdChanged_positive_naturalIdIsNotInChanges() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      Map.of(PERSONAL_NAME_TITLE, new AuthorityChange(PERSONAL_NAME_TITLE, "n", "o"),
        PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.isNaturalIdChanged();

    assertFalse(actual);
  }

  @Test
  void isOnlyNaturalIdChanged_positive_onlyNaturalIdIsInChanges() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o")),
      Map.of(), 1);

    var actual = holder.isOnlyNaturalIdChanged();

    assertTrue(actual);
  }

  @Test
  void isOnlyNaturalIdChanged_positive_notOnlyNaturalIdIsInChanges() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o"),
        PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.isOnlyNaturalIdChanged();

    assertFalse(actual);
  }

  @Test
  void isOnlyNaturalIdChanged_positive_naturalIdIsNotInChanges() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      Map.of(PERSONAL_NAME_TITLE, new AuthorityChange(PERSONAL_NAME_TITLE, "n", "o"),
        PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.isOnlyNaturalIdChanged();

    assertFalse(actual);
  }

  @Test
  void getFieldChange_positive_onlyFieldChange() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.getFieldChange();

    assertEquals(PERSONAL_NAME, actual);
  }

  @Test
  void getFieldChange_positive_fieldAndNaturalIdChanges() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent()
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o"),
        PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.getFieldChange();

    assertEquals(PERSONAL_NAME, actual);
  }

  @Test
  void getChangeType_positive_headingTypeChanged() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent().type(InventoryEventType.UPDATE.toString())
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      Map.of(PERSONAL_NAME_TITLE, new AuthorityChange(PERSONAL_NAME_TITLE, "n", "o"),
        PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.getChangeType();

    assertEquals(AuthorityChangeType.DELETE, actual);
  }

  @Test
  void getChangeType_positive_headingChanged() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent().type(InventoryEventType.UPDATE.toString())
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.getChangeType();

    assertEquals(AuthorityChangeType.UPDATE, actual);
  }

  @Test
  void getChangeType_positive_authorityDeleted() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent().type(InventoryEventType.DELETE.toString())
        ._new(new AuthorityInventoryRecord())
        .old(new AuthorityInventoryRecord()),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, null, "o")),
      Map.of(), 1);

    var actual = holder.getChangeType();

    assertEquals(AuthorityChangeType.DELETE, actual);
  }

  @Test
  void toAuthorityDataStat_positive_headingTypeChanged() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent().type(InventoryEventType.UPDATE.toString())
        ._new(new AuthorityInventoryRecord().naturalId("n"))
        .old(new AuthorityInventoryRecord().naturalId("o")),
      Map.of(CORPORATE_NAME, new AuthorityChange(CORPORATE_NAME, "n", null),
        PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, null, "o")),
      Map.of(PERSONAL_NAME, "100", CORPORATE_NAME, "101"), 1);

    var actual = holder.toAuthorityDataStat();

    assertThat(actual)
      .extracting(STAT_OBJ_PROPERTIES)
      .containsExactly(AuthorityDataStatAction.UPDATE_HEADING, "o", "n", "100", "101", "o", "n", null, null, 1);
  }

  @Test
  void toAuthorityDataStat_positive_headingChanged() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent().type(InventoryEventType.UPDATE.toString())
        ._new(new AuthorityInventoryRecord().naturalId("n"))
        .old(new AuthorityInventoryRecord().naturalId("n")),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(PERSONAL_NAME, "100"), 1);

    var actual = holder.toAuthorityDataStat();

    assertThat(actual)
      .extracting(STAT_OBJ_PROPERTIES)
      .containsExactly(AuthorityDataStatAction.UPDATE_HEADING, "o", "n", "100", "100", "n", "n", null, null, 1);
  }

  @Test
  void toAuthorityDataStat_positive_headingAndNaturalIdChangedChanged() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent().type(InventoryEventType.UPDATE.toString())
        ._new(new AuthorityInventoryRecord().naturalId("n"))
        .old(new AuthorityInventoryRecord().naturalId("o")),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o"),
        NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o")),
      Map.of(PERSONAL_NAME, "100"), 1);

    var actual = holder.toAuthorityDataStat();

    assertThat(actual)
      .extracting(STAT_OBJ_PROPERTIES)
      .containsExactly(AuthorityDataStatAction.UPDATE_HEADING, "o", "n", "100", "100", "o", "n", null, null, 1);
  }

  @Test
  void toAuthorityDataStat_positive_authorityDeleted() {
    var holder = new AuthorityChangeHolder(
      new InventoryEvent().type(InventoryEventType.DELETE.toString())
        .old(new AuthorityInventoryRecord().naturalId("o")),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, null, "o")),
      Map.of(PERSONAL_NAME, "100"), 1);

    var actual = holder.toAuthorityDataStat();

    assertThat(actual)
      .extracting(STAT_OBJ_PROPERTIES)
      .containsExactly(AuthorityDataStatAction.DELETE, "o", null, "100", "100", "o", null, null, null, 1);

  }
}
