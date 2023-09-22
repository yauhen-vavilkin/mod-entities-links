package org.folio.entlinks.service.messaging.authority.model;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField.CORPORATE_NAME;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField.NATURAL_ID;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField.PERSONAL_NAME;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField.PERSONAL_NAME_TITLE;
import static org.folio.entlinks.service.reindex.event.DomainEventType.DELETE;
import static org.folio.entlinks.service.reindex.event.DomainEventType.UPDATE;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.Metadata;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.folio.entlinks.integration.dto.AuthorityDomainEvent;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class AuthorityChangeHolderTest {

  public static final String[] STAT_OBJ_PROPERTIES =
    {"action", "headingOld", "headingNew", "headingTypeOld", "headingTypeNew",
     "authorityNaturalIdOld", "authorityNaturalIdNew", "authoritySourceFileOld", "authoritySourceFileNew",
     "lbTotal", "startedByUserId"};

  @Test
  void getNewNaturalId_positive() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto().naturalId("o"), new AuthorityDto().naturalId("n"),
        UPDATE, TENANT_ID),
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
      new AuthorityDomainEvent(null, new AuthorityDto().sourceFileId(sourceFileIdOld),
        new AuthorityDto().sourceFileId(sourceFileIdNew), UPDATE, TENANT_ID),
      Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o")),
      Map.of(), 1);

    var actual = holder.getNewSourceFileId();

    assertEquals(sourceFileIdNew, actual);
  }

  @Test
  void isNaturalIdChanged_positive_naturalIdIsInChanges() {
    var holder =
      new AuthorityChangeHolder(
        new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), UPDATE, TENANT_ID),
        Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o"),
          PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
        Map.of(), 1);

    var actual = holder.isNaturalIdChanged();

    assertTrue(actual);
  }

  @Test
  void isNaturalIdChanged_positive_naturalIdIsNotInChanges() {
    var holder =
      new AuthorityChangeHolder(
        new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), UPDATE, TENANT_ID),
        Map.of(PERSONAL_NAME_TITLE, new AuthorityChange(PERSONAL_NAME_TITLE, "n", "o"),
          PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
        Map.of(), 1);

    var actual = holder.isNaturalIdChanged();

    assertFalse(actual);
  }

  @Test
  void isOnlyNaturalIdChanged_positive_onlyNaturalIdIsInChanges() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), UPDATE, TENANT_ID),
      Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o")),
      Map.of(), 1);

    var actual = holder.isOnlyNaturalIdChanged();

    assertTrue(actual);
  }

  @Test
  void isOnlyNaturalIdChanged_positive_notOnlyNaturalIdIsInChanges() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), UPDATE, TENANT_ID),
      Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o"),
        PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.isOnlyNaturalIdChanged();

    assertFalse(actual);
  }

  @Test
  void isOnlyNaturalIdChanged_positive_naturalIdIsNotInChanges() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), UPDATE, TENANT_ID),
      Map.of(PERSONAL_NAME_TITLE, new AuthorityChange(PERSONAL_NAME_TITLE, "n", "o"),
        PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.isOnlyNaturalIdChanged();

    assertFalse(actual);
  }

  @Test
  void getFieldChange_positive_onlyFieldChange() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), UPDATE, TENANT_ID),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.getFieldChange();

    assertEquals(PERSONAL_NAME, actual);
  }

  @Test
  void getFieldChange_positive_fieldAndNaturalIdChanges() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), UPDATE, TENANT_ID),
      Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o"),
        PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.getFieldChange();

    assertEquals(PERSONAL_NAME, actual);
  }

  @Test
  void getChangeType_positive_headingTypeChanged() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), UPDATE, TENANT_ID),
      Map.of(PERSONAL_NAME_TITLE, new AuthorityChange(PERSONAL_NAME_TITLE, "n", "o"),
        PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.getChangeType();

    assertEquals(AuthorityChangeType.DELETE, actual);
  }

  @Test
  void getChangeType_positive_headingChanged() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), UPDATE, TENANT_ID),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(), 1);

    var actual = holder.getChangeType();

    assertEquals(AuthorityChangeType.UPDATE, actual);
  }

  @Test
  void getChangeType_positive_authorityDeleted() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), DELETE, TENANT_ID),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, null, "o")),
      Map.of(), 1);

    var actual = holder.getChangeType();

    assertEquals(AuthorityChangeType.DELETE, actual);
  }

  @Test
  void toAuthorityDataStat_positive_headingTypeChanged() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto().naturalId("o"), new AuthorityDto().naturalId("n"), UPDATE,
        TENANT_ID),
      Map.of(CORPORATE_NAME, new AuthorityChange(CORPORATE_NAME, "n", null),
        PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, null, "o")),
      Map.of(PERSONAL_NAME, "100", CORPORATE_NAME, "101"), 1);

    var actual = holder.toAuthorityDataStat();

    assertThat(actual)
      .extracting(STAT_OBJ_PROPERTIES)
      .containsExactly(AuthorityDataStatAction.UPDATE_HEADING, "o", "n", "100", "101", "o", "n", null, null, 1, null);
  }

  @Test
  void toAuthorityDataStat_positive_headingChanged() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto().naturalId("n"), new AuthorityDto().naturalId("n"), UPDATE,
        TENANT_ID),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o")),
      Map.of(PERSONAL_NAME, "100"), 1);

    var actual = holder.toAuthorityDataStat();

    assertThat(actual)
      .extracting(STAT_OBJ_PROPERTIES)
      .containsExactly(AuthorityDataStatAction.UPDATE_HEADING, "o", "n", "100", "100", "n", "n", null, null, 1, null);
  }

  @Test
  void toAuthorityDataStat_positive_headingAndNaturalIdChangedChanged() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto().naturalId("o"), new AuthorityDto().naturalId("n"), UPDATE,
        TENANT_ID),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o"),
        NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o")),
      Map.of(PERSONAL_NAME, "100"), 1);

    var actual = holder.toAuthorityDataStat();

    assertThat(actual)
      .extracting(STAT_OBJ_PROPERTIES)
      .containsExactly(AuthorityDataStatAction.UPDATE_HEADING, "o", "n", "100", "100", "o", "n", null, null, 1, null);
  }

  @Test
  void toAuthorityDataStat_positive_authorityDeleted() {
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto().naturalId("o"), null, DELETE, TENANT_ID),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, null, "o")),
      Map.of(PERSONAL_NAME, "100"), 1);

    var actual = holder.toAuthorityDataStat();

    assertThat(actual)
      .extracting(STAT_OBJ_PROPERTIES)
      .containsExactly(AuthorityDataStatAction.DELETE, "o", null, "100", "100", "o", null, null, null, 1, null);
  }

  @Test
  void toAuthorityDataStat_positive_metadataGiven() {
    UUID updatedByUserId = UUID.randomUUID();
    var holder = new AuthorityChangeHolder(
      new AuthorityDomainEvent(null, new AuthorityDto().naturalId("o"), new AuthorityDto().naturalId("n").metadata(
        new Metadata().updatedByUserId(updatedByUserId)), UPDATE, TENANT_ID),
      Map.of(PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "n", "o"),
        NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o")),
      Map.of(PERSONAL_NAME, "100"), 1);

    var actual = holder.toAuthorityDataStat();

    assertThat(actual)
      .extracting(STAT_OBJ_PROPERTIES)
      .containsExactly(AuthorityDataStatAction.UPDATE_HEADING, "o", "n", "100", "100", "o", "n", null, null, 1,
        updatedByUserId);
  }

  @Test
  void getFieldChange_null_onlyNaturalIdChanges() {
    var holder =
      new AuthorityChangeHolder(
        new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), UPDATE, TENANT_ID),
        Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "n", "o")),
        Map.of(), 1);

    var actual = holder.getFieldChange();

    assertNull(actual);
  }

  @Test
  void getFieldChange_null_noFieldChange() {
    var holder =
      new AuthorityChangeHolder(
        new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), UPDATE, TENANT_ID),
        Map.of(), Map.of(), 1);

    var actual = holder.getFieldChange();

    assertNull(actual);
  }

  @Test
  void getFieldChange_noFieldChange() {
    var holder =
      new AuthorityChangeHolder(
        new AuthorityDomainEvent(null, new AuthorityDto(), new AuthorityDto(), UPDATE, TENANT_ID),
        Map.of(),
        Map.of(), 1);

    var actual = holder.getFieldChange();

    assertNull(actual);
  }
}
