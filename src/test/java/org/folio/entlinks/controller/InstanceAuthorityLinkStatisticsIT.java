package org.folio.entlinks.controller;

import static org.folio.entlinks.domain.dto.AuthorityDataStatActionDto.UPDATE_HEADING;
import static org.folio.support.TestDataUtils.Link.TAGS;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.folio.support.base.TestConstants.authorityStatsEndpoint;
import static org.folio.support.base.TestConstants.inventoryAuthorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecordMetadata;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.base.IntegrationTestBase;
import org.folio.support.base.TestConstants;
import org.junit.jupiter.api.Test;

@IntegrationTest
@DatabaseCleanup(tables = {DatabaseHelper.AUTHORITY_DATA_STAT_TABLE,
                           DatabaseHelper.INSTANCE_AUTHORITY_LINK_TABLE,
                           DatabaseHelper.AUTHORITY_DATA_TABLE})
class InstanceAuthorityLinkStatisticsIT extends IntegrationTestBase {

  private static final UUID SOURCE_FILE_ID = UUID.randomUUID();
  private static final OffsetDateTime TO_DATE = OffsetDateTime.of(LocalDateTime.now().plusHours(1), ZoneOffset.UTC);
  private static final OffsetDateTime FROM_DATE = TO_DATE.minusMonths(1);

  @Test
  @SneakyThrows
  void getAuthDataStat_positive_whenStatsIsEmpty() {
    doGet(authorityStatsEndpoint(UPDATE_HEADING, FROM_DATE, TO_DATE, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.stats[0]").doesNotExist());
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_positive() {
    var instanceId = UUID.randomUUID();
    var authorityId = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f325e");
    var link = new TestDataUtils.Link(authorityId, TAGS[0]);

    // save link
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId, link)), instanceId);
    // send update event to store authority data stat
    sendInventoryAuthorityEvent(authorityId, TestConstants.UPDATE_TYPE);
    //await until stat saved to database
    awaitUntilAsserted(() -> assertEquals(1,
      databaseHelper.countRows(DatabaseHelper.AUTHORITY_DATA_STAT_TABLE, TENANT_ID)));

    // send update event to store another authority data stat
    sendInventoryAuthorityEvent(authorityId, TestConstants.UPDATE_TYPE);
    //await until stat saved to database
    awaitUntilAsserted(() -> assertEquals(2,
      databaseHelper.countRows(DatabaseHelper.AUTHORITY_DATA_STAT_TABLE, TENANT_ID)));

    doGet(authorityStatsEndpoint(UPDATE_HEADING, FROM_DATE, TO_DATE, 1))
      .andExpect(status().is2xxSuccessful())
      .andExpect(jsonPath("$.next", notNullValue()))
      .andExpect(jsonPath("$.stats[0].action", is(UPDATE_HEADING.name())))
      .andExpect(jsonPath("$.stats[0].authorityId", is(authorityId.toString())))
      .andExpect(jsonPath("$.stats[0].lbFailed", is(0)))
      .andExpect(jsonPath("$.stats[0].lbUpdated", is(0)))
      .andExpect(jsonPath("$.stats[0].lbTotal", is(1)))
      .andExpect(jsonPath("$.stats[0].naturalIdOld", is("naturalId")))
      .andExpect(jsonPath("$.stats[0].naturalIdNew", is("naturalId")))
      .andExpect(jsonPath("$.stats[0].sourceFileNew", is(SOURCE_FILE_ID.toString())))
      .andExpect(jsonPath("$.stats[0].headingOld", is("personal name")))
      .andExpect(jsonPath("$.stats[0].headingNew", is("new personal name")))
      .andExpect(jsonPath("$.stats[0].headingTypeOld", is("100")))
      .andExpect(jsonPath("$.stats[0].headingTypeNew", is("100")))
      .andExpect(jsonPath("$.stats[0].metadata.startedByUserId", is(USER_ID)))
      .andExpect(jsonPath("$.stats[0].metadata.startedByUserLastName", is("Doe")))
      .andExpect(jsonPath("$.stats[0].metadata.startedByUserFirstName", is("John")))
      .andExpect(jsonPath("$.stats[0].metadata.startedAt", notNullValue()));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_positive_whenAuthorityWasDeleted() {
    var instanceId = UUID.randomUUID();
    var authorityId = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f325e");
    var link = new TestDataUtils.Link(authorityId, TAGS[0]);

    // save link
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId, link)), instanceId);
    // send update event to store authority data stats
    sendInventoryAuthorityEvent(authorityId, TestConstants.UPDATE_TYPE);
    //await until stat saved to database
    awaitUntilAsserted(() -> assertEquals(1,
      databaseHelper.countRows(DatabaseHelper.AUTHORITY_DATA_STAT_TABLE, TENANT_ID)));

    // send delete event to mark authority as deleted
    sendInventoryAuthorityEvent(authorityId, TestConstants.DELETE_TYPE);
    //await until stat saved to database
    awaitUntilAsserted(() -> assertEquals(2,
      databaseHelper.countRows(DatabaseHelper.AUTHORITY_DATA_STAT_TABLE, TENANT_ID)));

    doGet(authorityStatsEndpoint(UPDATE_HEADING, FROM_DATE, TO_DATE, 1))
      .andExpect(status().is2xxSuccessful())
      .andExpect(jsonPath("$.stats[0]").doesNotExist());
  }

  private void sendInventoryAuthorityEvent(UUID authorityId, String type) {
    var authUpdateEvent = TestDataUtils.authorityEvent(type,
      new AuthorityInventoryRecord().id(authorityId).personalName("new personal name").naturalId("naturalId")
        .sourceFileId(SOURCE_FILE_ID)
        .metadata(new AuthorityInventoryRecordMetadata().updatedByUserId(UUID.fromString(USER_ID))),
      new AuthorityInventoryRecord().id(authorityId).personalName("personal name").naturalId("naturalId"));
    sendKafkaMessage(inventoryAuthorityTopic(), authorityId.toString(), authUpdateEvent);

  }
}
