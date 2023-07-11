package org.folio.entlinks.service.links;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.folio.entlinks.domain.dto.LinksChangeEvent.TypeEnum.DELETE;
import static org.folio.entlinks.domain.dto.LinksChangeEvent.TypeEnum.UPDATE;
import static org.folio.support.TestDataUtils.getAuthorityRecordsCollection;
import static org.folio.support.TestDataUtils.links;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.domain.dto.StrippedParsedRecordCollection;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.repository.InstanceLinkRepository;
import org.folio.entlinks.exception.DeletedLinkingAuthorityException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.integration.internal.AuthoritySourceFilesService;
import org.folio.entlinks.integration.internal.SearchService;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.spring.test.type.UnitTest;
import org.folio.support.TestDataUtils.Link;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Created to test renovate logic separately from regular update.
 * Merge with InstanceAuthorityLinkingServiceTest when renovate fixed / delete in case approach not needed.
 * Problems with renovation described in MODELINKS-113.
 * */
@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceAuthorityLinkingServiceRenovateTest {

  private final AuthoritySourceFilesService sourceFilesService = mock(AuthoritySourceFilesService.class);
  private final RenovateLinksService renovateLinksService = spy(new RenovateLinksService(sourceFilesService));

  @Mock
  private InstanceLinkRepository instanceLinkRepository;
  @Mock
  private AuthorityDataService authorityDataService;
  @Mock
  private SearchService searchService;
  @Mock
  private SourceStorageClient sourceStorageClient;
  @Mock
  private InstanceAuthorityLinkingRulesService linkingRulesService;
  @Spy
  private AuthorityRuleValidationService authorityRuleValidationService;
  @Mock
  private EventProducer<LinksChangeEvent> eventProducer;

  @InjectMocks
  private InstanceAuthorityLinkingService service;

  @Test
  void updateLinksRenovate_positive_saveIncomingLinks_whenAnyExist() {
    final var instanceId = randomUUID();
    final var existedLinks = Collections.<InstanceAuthorityLink>emptyList();
    final var incomingLinks = links(instanceId, Link.of(0, 0), Link.of(1, 1));

    when(linkingRulesService.getLinkingRules()).thenReturn(incomingLinks.stream()
      .map(InstanceAuthorityLink::getLinkingRule)
      .toList());
    mockAuthorities(incomingLinks);
    when(instanceLinkRepository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());

    service.updateLinksWithRenovation(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    var eventsCaptor = linksEventCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());
    verify(eventProducer, times(1)).sendMessages(eventsCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(2)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1]);

    assertThat(deleteCaptor.getValue()).isEmpty();

    var events = eventsCaptor.getValue();
    assertThat(events).hasSize(2)
      .extracting(LinksChangeEvent::getType)
      .allMatch(UPDATE::equals);

    events.forEach(event -> assertThat(event.getSubfieldsChanges().get(0).getSubfields())
      .anyMatch(subfieldChange -> subfieldChange.getCode().equals("0")
        && incomingLinks.stream()
        .anyMatch(link -> event.getSubfieldsChanges().get(0).getField().equals(link.getLinkingRule().getBibField())
          && subfieldChange.getValue().equals(link.getAuthorityData().getNaturalId())))
      .anyMatch(subfieldChange -> subfieldChange.getCode().equals("a")
        && subfieldChange.getValue().equals("test")));
  }

  @Test
  void updateLinksRenovate_positive_deleteAllLinks_whenIncomingIsEmpty() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId, Link.of(0, 0), Link.of(1, 1));
    final var incomingLinks = Collections.<InstanceAuthorityLink>emptyList();

    when(instanceLinkRepository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());

    service.updateLinksWithRenovation(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());
    verifyNoInteractions(eventProducer);

    assertThat(saveCaptor.getValue()).isEmpty();

    assertThat(deleteCaptor.getValue()).hasSize(2)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1]);
  }

  @Test
  void updateLinksRenovate_positive_deleteAllExistedAndSaveAllIncomingLinks() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    );
    final var incomingLinks = links(instanceId,
      Link.of(0, 1),
      Link.of(1, 0),
      Link.of(2, 3),
      Link.of(3, 2)
    );

    when(linkingRulesService.getLinkingRules()).thenReturn(incomingLinks.stream()
      .map(InstanceAuthorityLink::getLinkingRule)
      .toList());
    mockAuthorities(incomingLinks);
    when(instanceLinkRepository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());

    service.updateLinksWithRenovation(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    var eventsCaptor = linksEventCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());
    verify(eventProducer, times(1)).sendMessages(eventsCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).hasSize(4)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(eventsCaptor.getValue()).hasSize(4)
      .extracting(LinksChangeEvent::getType)
      .allMatch(UPDATE::equals);
  }

  @Test
  void updateLinksRenovate_positive_saveOnlyNewLinks() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1)
    );
    final var incomingLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    );

    when(linkingRulesService.getLinkingRules()).thenReturn(incomingLinks.stream()
      .map(InstanceAuthorityLink::getLinkingRule)
      .toList());
    mockAuthorities(incomingLinks);
    when(instanceLinkRepository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());

    service.updateLinksWithRenovation(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    var eventsCaptor = linksEventCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());
    verify(eventProducer, times(1)).sendMessages(eventsCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).isEmpty();

    assertThat(eventsCaptor.getValue()).hasSize(4)
      .extracting(LinksChangeEvent::getType)
      .allMatch(UPDATE::equals);
  }

  @Test
  void updateLinksRenovate_positive_deleteAndSaveLinks_whenHaveDifference() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    );
    final var incomingLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    );

    when(linkingRulesService.getLinkingRules()).thenReturn(incomingLinks.stream()
      .map(InstanceAuthorityLink::getLinkingRule)
      .toList());
    mockAuthorities(incomingLinks);
    when(instanceLinkRepository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());

    service.updateLinksWithRenovation(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    var eventsCaptor = linksEventCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());
    verify(eventProducer, times(1)).sendMessages(eventsCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).hasSize(2)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[2], Link.TAGS[3]);

    assertThat(eventsCaptor.getValue()).hasSize(4)
      .extracting(LinksChangeEvent::getType)
      .allMatch(UPDATE::equals);
  }

  @Test
  void updateLinksRenovate_positive_deleteAndSaveLinks_whenSomeAuthorityDeleted() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    );
    final var incomingLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    );

    when(linkingRulesService.getLinkingRules()).thenReturn(incomingLinks.stream()
      .map(InstanceAuthorityLink::getLinkingRule)
      .toList());
    mockAuthorities(incomingLinks.subList(0, incomingLinks.size() - 1));
    when(instanceLinkRepository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());

    service.updateLinksWithRenovation(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    var eventsCaptor = linksEventCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());
    verify(eventProducer, times(1)).sendMessages(eventsCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(3)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).hasSize(2)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[2], Link.TAGS[3]);

    var events = eventsCaptor.getAllValues();
    assertThat(events.get(0)).hasSize(4)
      .extracting(LinksChangeEvent::getType)
      .containsExactlyInAnyOrder(UPDATE, UPDATE, UPDATE, DELETE);
  }

  @Test
  void updateLinksRenovate_negative_whenAuthoritiesAreDeletedForIncomingLinks() {
    final var instanceId = randomUUID();
    var incomingLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    );
    var authorityIds = incomingLinks.stream()
      .map(link -> link.getAuthorityData().getId())
      .collect(Collectors.toSet());
    var deletedAuthorityIds = incomingLinks.stream()
      .map(link -> link.getAuthorityData().getId())
      .limit(2)
      .collect(Collectors.toSet());
    var authorityDataMock = deletedAuthorityIds.stream()
      .map(authorityId -> new AuthorityData(authorityId, "deleted", true))
      .toList();

    when(authorityDataService.getByIdAndDeleted(authorityIds, true)).thenReturn(authorityDataMock);

    var exception = Assertions.assertThrows(DeletedLinkingAuthorityException.class,
      () -> service.updateLinksWithRenovation(instanceId, incomingLinks));

    assertThat(exception)
      .hasMessage("Cannot save links to deleted authorities.")
      .extracting(RequestBodyValidationException::getInvalidParameters)
      .returns(2, from(List::size));
  }

  @Test
  void updateLinksRenovate_positive_deleteAndSaveLinks_whenSomeAuthorityChangedToNotLinkable() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    );
    final var incomingLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    );
    var linksForValidAuthorities = incomingLinks.subList(1, incomingLinks.size());
    var linksForInvalidAuthorities = incomingLinks.subList(0, 1);
    var authorityRecordsMock = getAuthorityRecordsCollection(linksForValidAuthorities, linksForInvalidAuthorities);

    when(linkingRulesService.getLinkingRules()).thenReturn(incomingLinks.stream()
      .map(InstanceAuthorityLink::getLinkingRule)
      .toList());
    mockAuthorities(incomingLinks, authorityRecordsMock);
    when(instanceLinkRepository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());

    service.updateLinksWithRenovation(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    var eventsCaptor = linksEventCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());
    verify(eventProducer, times(1)).sendMessages(eventsCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(3)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).hasSize(3)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[2], Link.TAGS[3]);

    var events = eventsCaptor.getAllValues();
    assertThat(events.get(0)).hasSize(4)
      .extracting(LinksChangeEvent::getType)
      .containsExactlyInAnyOrder(UPDATE, UPDATE, UPDATE, DELETE);
  }

  private void mockAuthorities(List<InstanceAuthorityLink> links) {
    mockAuthorities(links, getAuthorityRecordsCollection(links));
  }

  @SuppressWarnings("unchecked")
  private void mockAuthorities(List<InstanceAuthorityLink> links, StrippedParsedRecordCollection authorityRecords) {
    final var authorityDataSet = links.stream()
      .map(InstanceAuthorityLink::getAuthorityData)
      .toList();

    when(searchService.searchAuthoritiesByIds(any())).thenReturn(authorityDataSet);
    when(sourceStorageClient.fetchParsedRecordsInBatch(any())).thenReturn(authorityRecords);
    when(authorityDataService.saveAll(any(Collection.class)))
      .thenAnswer(invocation -> ((Collection<AuthorityData>) invocation.getArgument(0)).stream()
        .collect(Collectors.toMap(AuthorityData::getId, a -> a)));
  }

  private ArgumentCaptor<List<InstanceAuthorityLink>> linksCaptor() {
    @SuppressWarnings("unchecked") var listClass = (Class<List<InstanceAuthorityLink>>) (Class<?>) List.class;
    return ArgumentCaptor.forClass(listClass);
  }

  private ArgumentCaptor<List<LinksChangeEvent>> linksEventCaptor() {
    @SuppressWarnings("unchecked") var listClass = (Class<List<LinksChangeEvent>>) (Class<?>) List.class;
    return ArgumentCaptor.forClass(listClass);
  }

}
