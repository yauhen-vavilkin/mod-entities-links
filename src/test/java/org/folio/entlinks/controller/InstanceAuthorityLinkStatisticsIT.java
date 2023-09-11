package org.folio.entlinks.controller;

import static java.util.Collections.singletonList;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.folio.entlinks.domain.dto.LinkAction.UPDATE_HEADING;
import static org.folio.support.DatabaseHelper.AUTHORITY_DATA_STAT_TABLE;
import static org.folio.support.MatchUtils.errorCodeMatch;
import static org.folio.support.MatchUtils.errorMessageMatch;
import static org.folio.support.MatchUtils.errorTotalMatch;
import static org.folio.support.MatchUtils.errorTypeMatch;
import static org.folio.support.MatchUtils.statsMatch;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.TestDataUtils.Link.TAGS;
import static org.folio.support.TestDataUtils.NATURAL_IDS;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.TestDataUtils.stats;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.authorityStatsEndpoint;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.folio.support.base.TestConstants.linksStatsInstanceEndpoint;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ThreadUtils;
import org.awaitility.Durations;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.BibStatsDtoCollection;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.exception.type.ErrorType;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.TestDataUtils.Link;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
@DatabaseCleanup(tables = {
  AUTHORITY_DATA_STAT_TABLE,
  DatabaseHelper.INSTANCE_AUTHORITY_LINK_TABLE,
  DatabaseHelper.AUTHORITY_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE})
class InstanceAuthorityLinkStatisticsIT extends IntegrationTestBase {

  private static final UUID AUTHORITY_ID = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f325e");
  private static final OffsetDateTime TO_DATE = OffsetDateTime.of(LocalDateTime.now().plusHours(1), ZoneOffset.UTC);
  private static final OffsetDateTime FROM_DATE = TO_DATE.minusMonths(1);

  private static final List<UUID> INSTANCE_IDS = List.of(
    UUID.fromString("fea1c418-ba1f-438e-85bb-c6ae1011bf5c"),
    UUID.fromString("e083463e-96d4-4fa0-8ee1-13bfd4f674cf"),
    UUID.fromString("68de093d-8c0d-44c2-b3a8-79393f6cb195")
  );
  private static final List<String> INSTANCE_TITLES = List.of("title1", "title2", "title3");
  private static final Link[] LINKS = new Link[] {
      Link.of(0, 0, NATURAL_IDS[0]),
      Link.of(1, 1, NATURAL_IDS[1])
  };

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @BeforeEach
  public void setup() {
    var sourceFile = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, sourceFile);
    var authority1 = authority(0, 0);
    databaseHelper.saveAuthority(TENANT_ID, authority1);
    var authority2 = authority(1, 0);
    databaseHelper.saveAuthority(TENANT_ID, authority2);
    var authority3 = authority(0, 0);
    authority3.setId(AUTHORITY_ID);
    authority3.setNaturalId(AUTHORITY_ID.toString());
    authority3.setAuthoritySourceFile(null);
    databaseHelper.saveAuthority(TENANT_ID, authority3);
  }

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
    var link = new Link(AUTHORITY_ID, TAGS[1]);

    // save link
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId, link)), instanceId);
    // send update event to store authority data stat
    sendAuthorityUpdateEvent();
    // wait until stat is saved to database
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        assertEquals(1, databaseHelper.countRows(AUTHORITY_DATA_STAT_TABLE, TENANT_ID))
    );

    // send update event to store another authority data stat
    sendAuthorityUpdateEvent();
    // wait until stat is saved to database
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        assertEquals(2, databaseHelper.countRows(AUTHORITY_DATA_STAT_TABLE, TENANT_ID))
    );

    doGet(authorityStatsEndpoint(UPDATE_HEADING, FROM_DATE, TO_DATE, 1))
      .andExpect(status().is2xxSuccessful())
      .andExpect(jsonPath("$.next", notNullValue()))
      .andExpect(jsonPath("$.stats[0].action", is(UPDATE_HEADING.name())))
      .andExpect(jsonPath("$.stats[0].authorityId", is(AUTHORITY_ID.toString())))
      .andExpect(jsonPath("$.stats[0].lbFailed", is(0)))
      .andExpect(jsonPath("$.stats[0].lbUpdated", is(0)))
      .andExpect(jsonPath("$.stats[0].lbTotal", is(1)))
      .andExpect(jsonPath("$.stats[0].naturalIdOld", is(AUTHORITY_ID.toString())))
      .andExpect(jsonPath("$.stats[0].naturalIdNew", is(AUTHORITY_ID.toString())))
      .andExpect(jsonPath("$.stats[0].sourceFileOld", is("Not specified")))
      .andExpect(jsonPath("$.stats[0].sourceFileNew", is("Not specified")))
      .andExpect(jsonPath("$.stats[0].headingOld", is("headingPersonalName updated")))
      .andExpect(jsonPath("$.stats[0].headingNew", is("headingPersonalName updated updated")))
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
    var link = new Link(AUTHORITY_ID, TAGS[0]);

    // save link
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId, link)), instanceId);
    // send update event to store authority data stats
    sendAuthorityUpdateEvent();
    // wait until stat is saved to database
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        assertEquals(1, databaseHelper.countRows(AUTHORITY_DATA_STAT_TABLE, TENANT_ID))
    );

    // send delete event to mark authority as deleted
    doDelete(authorityEndpoint(AUTHORITY_ID));
    // wait until stat saved to database
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        assertEquals(2, databaseHelper.countRows(AUTHORITY_DATA_STAT_TABLE, TENANT_ID))
    );

    doGet(authorityStatsEndpoint(UPDATE_HEADING, FROM_DATE, TO_DATE, 1))
      .andExpect(jsonPath("$.stats[0]").doesNotExist());
  }

  @Test
  void getLinkedBibUpdateStats_positive_noStatsFound() throws Exception {
    perform(getStatsRequest())
      .andExpect(statsMatch(empty()))
      .andExpect(nextMatch(null));
  }

  @Test
  void getLinkedBibUpdateStats_positive_noStatsFoundForStatus() throws Exception {
    var instanceId = INSTANCE_IDS.get(0);
    var links = linksDtoCollection(linksDto(instanceId, LINKS));
    doPut(linksInstanceEndpoint(), links, instanceId);

    var toDate = OffsetDateTime.now();
    var fromDate = toDate.minus(1, ChronoUnit.DAYS);
    perform(getStatsRequest(LinkStatus.ERROR, fromDate, toDate))
      .andExpect(statsMatch(empty()))
      .andExpect(nextMatch(null));
  }

  @Test
  void getLinkedBibUpdateStats_positive() throws Exception {
    var instanceId = INSTANCE_IDS.get(0);
    var links = linksDtoCollection(linksDto(instanceId, LINKS));
    doPut(linksInstanceEndpoint(), links, instanceId);

    var stats = stats(links.getLinks(), null, null, INSTANCE_TITLES.get(0));

    perform(getStatsRequest())
      .andExpect(statsMatch(hasSize(2)))
      .andExpect(statsMatch(stats))
      .andExpect(nextMatch(stats.getNext()));
  }

  @Test
  void getLinkedBibUpdateStats_positive_noParams() throws Exception {
    var instanceId = INSTANCE_IDS.get(0);
    var links = linksDtoCollection(linksDto(instanceId, LINKS));
    doPut(linksInstanceEndpoint(), links, instanceId);

    var stats = stats(links.getLinks(), null, null, INSTANCE_TITLES.get(0));

    perform(get(linksStatsInstanceEndpoint()))
      .andExpect(statsMatch(hasSize(2)))
      .andExpect(statsMatch(stats))
      .andExpect(nextMatch(stats.getNext()));
  }

  @Test
  void getLinkedBibUpdateStats_positive_differentInstances() throws Exception {
    var instanceId1 = INSTANCE_IDS.get(0);
    var instanceId2 = INSTANCE_IDS.get(1);
    var links1 = linksDtoCollection(linksDto(instanceId1, LINKS));
    var links2 = linksDtoCollection(linksDto(instanceId2, LINKS));
    doPut(linksInstanceEndpoint(), links1, instanceId1);
    doPut(linksInstanceEndpoint(), links2, instanceId2);

    var stats1 = stats(links1.getLinks(), null, null, INSTANCE_TITLES.get(0));
    var stats2 = stats(links2.getLinks(), null, null, INSTANCE_TITLES.get(1));
    var stats = new BibStatsDtoCollection()
      .stats(Stream.concat(stats2.getStats().stream(), stats1.getStats().stream()).toList())
      .next(null);

    perform(getStatsRequest())
      .andExpect(statsMatch(hasSize(4)))
      .andExpect(statsMatch(stats))
      .andExpect(nextMatch(stats.getNext()));
  }

  @Test
  void getLinkedBibUpdateStats_positive_withSkippedAndNext() throws Exception {
    var instanceId1 = INSTANCE_IDS.get(0);
    var links1 = linksDtoCollection(linksDto(instanceId1, LINKS));
    doPut(linksInstanceEndpoint(), links1, instanceId1);

    OffsetDateTime now = OffsetDateTime.now();
    String millisStr = String.valueOf(now.getNano()).substring(0, 3);
    long duration = 1000 - Integer.parseInt(millisStr);
    ThreadUtils.sleep(Duration.ofMillis(duration));

    final var fromDate = OffsetDateTime.now();

    var instanceId2 = INSTANCE_IDS.get(1);
    var links2 = linksDtoCollection(linksDto(instanceId2, LINKS));
    doPut(linksInstanceEndpoint(), links2, instanceId2);

    ThreadUtils.sleep(Duration.ofSeconds(1));

    var instanceId3 = INSTANCE_IDS.get(2);
    var links3 = linksDtoCollection(linksDto(instanceId3, LINKS));
    doPut(linksInstanceEndpoint(), links3, instanceId3);
    var toDate = OffsetDateTime.now();

    var stats1 = stats(links3.getLinks(), null, OffsetDateTime.now(), INSTANCE_TITLES.get(2))
      .getStats();
    var stats2 = stats(singletonList(links2.getLinks().get(1)),
      null, OffsetDateTime.now(), INSTANCE_TITLES.get(1))
      .getStats();
    var next = toDate.minus(1, ChronoUnit.SECONDS);
    var nextStartsWith = next.toString().substring(0, 19);
    var stats = new BibStatsDtoCollection()
      .stats(new LinkedList<>(stats1))
      .next(next);
    stats.getStats().add(stats2.get(0));

    perform(getStatsRequest(LinkStatus.ACTUAL, fromDate, toDate).param("limit", "3"))
      .andExpect(statsMatch(hasSize(3)))
      .andExpect(statsMatch(stats))
      .andExpect(jsonPath("$.next", startsWith(nextStartsWith)));
  }

  @Test
  void getLinkedBibUpdateStats_positive_onlyOneDateAndLinksSkipped() throws Exception {
    var instanceId1 = INSTANCE_IDS.get(0);
    var links1 = linksDtoCollection(linksDto(instanceId1, LINKS));
    doPut(linksInstanceEndpoint(), links1, instanceId1);

    ThreadUtils.sleep(Duration.ofSeconds(1));

    var fromDate = OffsetDateTime.now();
    var instanceId2 = INSTANCE_IDS.get(1);
    var links2 = linksDtoCollection(linksDto(instanceId2, LINKS));
    doPut(linksInstanceEndpoint(), links2, instanceId2);

    var stats = stats(links2.getLinks(), null, OffsetDateTime.now(), INSTANCE_TITLES.get(1));

    perform(get(linksStatsInstanceEndpoint()).param("fromDate", fromDate.toString()))
      .andExpect(statsMatch(hasSize(2)))
      .andExpect(statsMatch(stats))
      .andExpect(nextMatch(null));
  }

  @Test
  void getLinkedBibUpdateStats_negative_invalidDates() throws Exception {
    var fromDate = OffsetDateTime.now();
    var toDate = fromDate.minus(1, ChronoUnit.DAYS);
    perform(getStatsRequest(LinkStatus.ACTUAL, fromDate, toDate))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("RequestBodyValidationException")))
      .andExpect(errorCodeMatch(is(ErrorType.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("'to' date should be not less than 'from' date.")));
  }

  private MockHttpServletRequestBuilder getStatsRequest() {
    var toDate = OffsetDateTime.now();
    var fromDate = toDate.minus(1, ChronoUnit.DAYS);
    return getStatsRequest(LinkStatus.ACTUAL, fromDate, toDate);
  }

  private MockHttpServletRequestBuilder getStatsRequest(LinkStatus status,
                                                        OffsetDateTime fromDate, OffsetDateTime toDate) {
    return get(linksStatsInstanceEndpoint(status, fromDate, toDate));
  }

  private ResultMatcher nextMatch(OffsetDateTime next) {
    if (next == null) {
      return jsonPath("$.next").doesNotExist();
    }
    return jsonPath("$.next", is(next));
  }

  private void sendAuthorityUpdateEvent() throws UnsupportedEncodingException, JsonProcessingException {
    var content = doGet(authorityEndpoint(AUTHORITY_ID)).andReturn().getResponse().getContentAsString();
    var authorityDto = objectMapper.readValue(content, AuthorityDto.class);
    authorityDto.setPersonalName(authorityDto.getPersonalName() + " updated");
    doPut(authorityEndpoint(AUTHORITY_ID), authorityDto);
  }
}
