package org.folio.entlinks.controller.delegate;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.assertj.core.api.Assertions.tuple;
import static org.folio.entlinks.utils.DateUtils.fromTimestamp;
import static org.folio.support.TestDataUtils.links;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.TestDataUtils.stats;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.folio.entlinks.controller.converter.DataStatsMapper;
import org.folio.entlinks.controller.converter.InstanceAuthorityLinkMapper;
import org.folio.entlinks.domain.dto.BibStatsDtoCollection;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.LinksCountDto;
import org.folio.entlinks.domain.dto.UuidCollection;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.integration.internal.InstanceStorageService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.spring.test.type.UnitTest;
import org.folio.support.TestDataUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinkingServiceDelegateTest {

  private static final UUID INSTANCE_ID = randomUUID();

  private @Mock InstanceAuthorityLinkingService linkingService;
  private @Mock InstanceAuthorityLinkMapper mapper;
  private @Mock InstanceStorageService instanceService;
  private @Mock DataStatsMapper statsMapper;

  private @InjectMocks LinkingServiceDelegate delegate;

  @Test
  void getLinks_positive() {
    var linkData = TestDataUtils.Link.of(0, 0);
    var link = linkData.toEntity(INSTANCE_ID);
    var links = List.of(link);
    var linkDto = linkData.toDto(INSTANCE_ID);

    when(linkingService.getLinksByInstanceId(INSTANCE_ID)).thenReturn(links);
    when(mapper.convertToDto(links)).thenReturn(
      new InstanceLinkDtoCollection().links(List.of(linkDto)).totalRecords(1));

    var actual = delegate.getLinks(INSTANCE_ID);

    assertThat(actual).isNotNull()
      .extracting(InstanceLinkDtoCollection::getTotalRecords)
      .isEqualTo(1);

    assertThat(actual.getLinks())
      .hasSize(1)
      .containsExactlyInAnyOrder(linkDto);
  }

  @Test
  void getLinkedBibUpdateStats_positive() {
    var linksMock = links(3, "error");
    var linksForStats = linksMock.subList(0, 2);
    var instanceIds = linksForStats.stream()
      .map(InstanceAuthorityLink::getInstanceId)
      .map(UUID::toString)
      .toList();
    var instanceTitles = instanceIds.stream()
      .collect(Collectors.toMap(id -> id, id -> RandomStringUtils.randomAlphanumeric(5)));
    var nextLinkTime = fromTimestamp(linksMock.get(linksMock.size() - 1).getUpdatedAt());

    testGetLinkedBibUpdateStats_positive(linksMock, linksForStats, instanceIds, instanceTitles, nextLinkTime);
  }

  @Test
  void getLinkedBibUpdateStats_positive_noNext() {
    var linksMock = links(2, "error");
    var instanceIds = linksMock.stream()
      .map(InstanceAuthorityLink::getInstanceId)
      .map(UUID::toString)
      .toList();
    var instanceTitles = instanceIds.stream()
      .collect(Collectors.toMap(id -> id, id -> RandomStringUtils.randomAlphanumeric(5)));

    testGetLinkedBibUpdateStats_positive(linksMock, linksMock, instanceIds, instanceTitles, null);
  }

  @Test
  void getLinkedBibUpdateStats_positive_sameInstance() {
    var linksMock = links(2, "error");
    var instanceId = linksMock.get(0).getInstanceId();
    linksMock.get(1).setInstanceId(instanceId);
    var instanceTitle = RandomStringUtils.randomAlphanumeric(5);
    var instanceTitles = Map.of(instanceId.toString(), instanceTitle);

    testGetLinkedBibUpdateStats_positive(linksMock, linksMock,
      singletonList(instanceId.toString()), instanceTitles, null);
  }

  @Test
  void getLinkedBibUpdateStats_positive_noTitle() {
    var linksMock = links(2, "error");
    var instanceIds = linksMock.stream()
      .map(InstanceAuthorityLink::getInstanceId)
      .map(UUID::toString)
      .toList();
    var instanceTitles = Map.of(instanceIds.get(0), RandomStringUtils.randomAlphanumeric(5));

    testGetLinkedBibUpdateStats_positive(linksMock, linksMock, instanceIds, instanceTitles, null);
  }

  @Test
  void getLinkedBibUpdateStats_negative_invalidDates() {
    var status = LinkStatus.ACTUAL;
    var fromDate = OffsetDateTime.now();
    var toDate = fromDate.minus(1, ChronoUnit.DAYS);
    var limit = 2;

    var exception = Assertions.assertThrows(RequestBodyValidationException.class,
      () -> delegate.getLinkedBibUpdateStats(fromDate, toDate, status, limit));

    assertThat(exception)
      .hasMessage("'to' date should be not less than 'from' date.")
      .extracting(RequestBodyValidationException::getInvalidParameters)
      .returns(2, from(List::size));
  }

  @Test
  void updateLinks_positive() {
    final var links = links(INSTANCE_ID,
      TestDataUtils.Link.of(0, 0),
      TestDataUtils.Link.of(1, 1),
      TestDataUtils.Link.of(2, 2),
      TestDataUtils.Link.of(3, 3)
    );
    final var dtoCollection = linksDtoCollection(linksDto(INSTANCE_ID,
      TestDataUtils.Link.of(0, 0),
      TestDataUtils.Link.of(1, 1),
      TestDataUtils.Link.of(2, 3),
      TestDataUtils.Link.of(3, 2)
    ));

    doNothing().when(linkingService).updateLinks(INSTANCE_ID, links);
    when(mapper.convertDto(dtoCollection.getLinks())).thenReturn(links);

    delegate.updateLinks(INSTANCE_ID, dtoCollection);

    verify(linkingService).updateLinks(INSTANCE_ID, links);
  }

  @Test
  void updateLinks_negative_whenInstanceIdIsNotSameForIncomingLinks() {
    var incomingLinks = linksDtoCollection(linksDto(randomUUID(),
      TestDataUtils.Link.of(0, 0),
      TestDataUtils.Link.of(1, 1),
      TestDataUtils.Link.of(2, 3),
      TestDataUtils.Link.of(3, 2)
    ));

    var exception = Assertions.assertThrows(RequestBodyValidationException.class,
      () -> delegate.updateLinks(INSTANCE_ID, incomingLinks));

    assertThat(exception)
      .hasMessage("Link should have instanceId = " + INSTANCE_ID)
      .extracting(RequestBodyValidationException::getInvalidParameters)
      .returns(4, from(List::size));
  }

  @Test
  void countLinksByAuthorityIds_positive() {
    var ids = List.of(randomUUID(), randomUUID(), randomUUID());

    when(linkingService.countLinksByAuthorityIds(new HashSet<>(ids))).thenReturn(
      Map.of(ids.get(0), 2, ids.get(1), 1));
    when(mapper.convert(anyMap())).thenCallRealMethod();

    var actual = delegate.countLinksByAuthorityIds(new UuidCollection().ids(ids));

    assertThat(actual.getLinks())
      .hasSize(ids.size())
      .extracting(LinksCountDto::getId, LinksCountDto::getTotalLinks)
      .containsExactlyInAnyOrder(tuple(ids.get(0), 2), tuple(ids.get(1), 1), tuple(ids.get(2), 0));
  }

  private void testGetLinkedBibUpdateStats_positive(List<InstanceAuthorityLink> linksMock,
                                                    List<InstanceAuthorityLink> linksForStats,
                                                    List<String> instanceIds,
                                                    Map<String, String> instanceTitles,
                                                    OffsetDateTime next) {
    var status = LinkStatus.ACTUAL;
    var fromDate = OffsetDateTime.now();
    var toDate = fromDate.plus(1, ChronoUnit.DAYS);
    var limit = 2;
    var expectedStats = stats(linksForStats);

    when(linkingService.getLinks(status, fromDate, toDate, limit + 1))
      .thenReturn(linksMock);
    when(statsMapper.convertToDto(linksForStats))
      .thenReturn(expectedStats);
    when(instanceService.getInstanceTitles(instanceIds))
      .thenReturn(instanceTitles);

    expectedStats.forEach(bibStatsDto -> {
      var instanceId = bibStatsDto.getInstanceId();
      bibStatsDto.setInstanceTitle(instanceTitles.get(instanceId.toString()));
    });

    var actual = delegate.getLinkedBibUpdateStats(fromDate, toDate, status, limit);

    assertThat(actual)
      .isEqualTo(new BibStatsDtoCollection()
        .stats(expectedStats)
        .next(next));
  }
}
