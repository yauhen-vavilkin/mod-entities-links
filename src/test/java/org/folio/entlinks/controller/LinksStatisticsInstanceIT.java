package org.folio.entlinks.controller;

import static java.util.Collections.singletonList;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.TestDataUtils.stats;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.folio.support.base.TestConstants.linksStatsInstanceEndpoint;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.ThreadUtils;
import org.folio.entlinks.domain.dto.BibStatsDto;
import org.folio.entlinks.domain.dto.BibStatsDtoCollection;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.exception.type.ErrorCode;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.base.IntegrationTestBase;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
@DatabaseCleanup(tables = DatabaseHelper.INSTANCE_AUTHORITY_LINK_TABLE)
class LinksStatisticsInstanceIT extends IntegrationTestBase {

  private static final List<UUID> INSTANCE_IDS = List.of(
    UUID.fromString("fea1c418-ba1f-438e-85bb-c6ae1011bf5c"),
    UUID.fromString("e083463e-96d4-4fa0-8ee1-13bfd4f674cf"),
    UUID.fromString("68de093d-8c0d-44c2-b3a8-79393f6cb195")
  );
  private static final List<String> INSTANCE_TITLES = List.of("title1", "title2", "title3");

  @Test
  void getLinkedBibUpdateStats_positive_noStatsFound() throws Exception {
    perform(getStatsRequest())
      .andExpect(statsMatch(empty()))
      .andExpect(nextMatch(null));
  }

  @Test
  void getLinkedBibUpdateStats_positive_noStatsFoundForStatus() throws Exception {
    var instanceId = INSTANCE_IDS.get(0);
    var links = linksDtoCollection(linksDto(instanceId,
      TestDataUtils.Link.of(0, 0), TestDataUtils.Link.of(1, 1)));
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
    var links = linksDtoCollection(linksDto(instanceId,
      TestDataUtils.Link.of(0, 0), TestDataUtils.Link.of(1, 1)));
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
    var links = linksDtoCollection(linksDto(instanceId,
      TestDataUtils.Link.of(0, 0), TestDataUtils.Link.of(1, 1)));
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
    var links1 = linksDtoCollection(linksDto(instanceId1,
      TestDataUtils.Link.of(0, 0), TestDataUtils.Link.of(1, 1)));
    var links2 = linksDtoCollection(linksDto(instanceId2,
      TestDataUtils.Link.of(0, 0), TestDataUtils.Link.of(1, 1)));
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
    var links1 = linksDtoCollection(linksDto(instanceId1,
      TestDataUtils.Link.of(0, 0), TestDataUtils.Link.of(1, 1)));
    doPut(linksInstanceEndpoint(), links1, instanceId1);

    final var fromDate = OffsetDateTime.now();
    ThreadUtils.sleep(Duration.ofSeconds(1));

    var instanceId2 = INSTANCE_IDS.get(1);
    var links2 = linksDtoCollection(linksDto(instanceId2,
      TestDataUtils.Link.of(0, 0), TestDataUtils.Link.of(1, 1)));
    doPut(linksInstanceEndpoint(), links2, instanceId2);

    ThreadUtils.sleep(Duration.ofSeconds(1));

    var instanceId3 = INSTANCE_IDS.get(2);
    var links3 = linksDtoCollection(linksDto(instanceId3,
      TestDataUtils.Link.of(0, 0), TestDataUtils.Link.of(1, 1)));
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
    var links1 = linksDtoCollection(linksDto(instanceId1,
      TestDataUtils.Link.of(0, 0), TestDataUtils.Link.of(1, 1)));
    doPut(linksInstanceEndpoint(), links1, instanceId1);

    ThreadUtils.sleep(Duration.ofSeconds(1));

    var fromDate = OffsetDateTime.now();
    var instanceId2 = INSTANCE_IDS.get(1);
    var links2 = linksDtoCollection(linksDto(instanceId2,
      TestDataUtils.Link.of(0, 0), TestDataUtils.Link.of(1, 1)));
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
      .andExpect(errorCodeMatch(is(ErrorCode.VALIDATION_ERROR.getValue())))
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

  private ResultMatcher statsMatch(Matcher<Collection<? extends BibStatsDto>> matcher) {
    return jsonPath("$.stats", matcher);
  }

  @SuppressWarnings("unchecked")
  private ResultMatcher statsMatch(BibStatsDtoCollection stats) {
    var statsMatchers = stats.getStats().stream()
      .map(LinksStatisticsInstanceIT.StatsMatcher::statsMatch)
      .toArray(Matcher[]::new);
    return jsonPath("$.stats", contains(statsMatchers));
  }

  private static final class StatsMatcher extends BaseMatcher<BibStatsDto> {

    private final BibStatsDto expectedStats;

    private StatsMatcher(BibStatsDto expectedStats) {
      this.expectedStats = expectedStats;
    }

    static LinksStatisticsInstanceIT.StatsMatcher statsMatch(BibStatsDto expectedStats) {
      return new LinksStatisticsInstanceIT.StatsMatcher(expectedStats);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean matches(Object actual) {
      if (actual instanceof LinkedHashMap actualStats) {
        return Objects.equals(expectedStats.getInstanceId().toString(), actualStats.get("instanceId"))
          && Objects.equals(expectedStats.getAuthorityNaturalId(), actualStats.get("authorityNaturalId"))
          && Objects.equals(expectedStats.getBibRecordTag(), actualStats.get("bibRecordTag"))
          && Objects.equals(expectedStats.getInstanceTitle(), actualStats.get("instanceTitle"))
          && expectedStats.getUpdatedAt().isAfter(OffsetDateTime.parse((String) actualStats.get("updatedAt")))
          && Objects.equals(expectedStats.getErrorCause(), actualStats.get("errorCause"));
      }

      return false;
    }

    @Override
    public void describeTo(Description description) {
      description.appendValue(expectedStats);
    }
  }

}
