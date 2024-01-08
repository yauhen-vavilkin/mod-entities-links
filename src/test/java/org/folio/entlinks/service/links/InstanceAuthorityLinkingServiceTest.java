package org.folio.entlinks.service.links;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.folio.support.TestDataUtils.links;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.projection.LinkCountView;
import org.folio.entlinks.domain.repository.InstanceLinkRepository;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.spring.testing.type.UnitTest;
import org.folio.support.TestDataUtils.Link;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceAuthorityLinkingServiceTest {

  @Mock
  private InstanceLinkRepository instanceLinkRepository;

  @Mock
  private AuthorityService authorityService;

  @InjectMocks
  private InstanceAuthorityLinkingService service;

  @Test
  void getLinksByInstanceId_positive_foundWhenExist() {
    var instanceId = randomUUID();
    var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    );

    when(instanceLinkRepository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);

    var result = service.getLinksByInstanceId(instanceId);

    assertThat(result)
      .hasSize(existedLinks.size())
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);
  }

  @Test
  void getLinksByInstanceId_positive_nothingFound() {
    var instanceId = randomUUID();
    var existedLinks = Collections.<InstanceAuthorityLink>emptyList();

    when(instanceLinkRepository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);

    var result = service.getLinksByInstanceId(instanceId);

    assertThat(result).isEmpty();
  }

  @Test
  void getLinksByAuthorityId_positive_foundWhenExist() {
    var instanceId = randomUUID();
    var links = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1)
    );

    var linkPage = new PageImpl<>(links);

    when(instanceLinkRepository.findByAuthorityId(any(UUID.class), any(Pageable.class))).thenReturn(linkPage);

    var result = service.getLinksByAuthorityId(UUID.randomUUID(), Pageable.ofSize(2));

    assertThat(result)
      .hasSize(links.size())
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1]);
  }

  @Test
  void getLinksByIds_positive_foundWhenExist() {
    var links = links(2);
    var ids = links.stream()
      .map(link -> link.getId().intValue())
      .toList();
    var longIds = links.stream()
      .map(InstanceAuthorityLink::getId)
      .toList();

    when(instanceLinkRepository.findAllById(longIds)).thenReturn(links);

    var result = service.getLinksByIds(ids);

    assertThat(result)
      .containsOnly(links.get(0), links.get(1));
  }

  @Test
  void updateLinks_positive_saveIncomingLinks_whenAnyExist() {
    final var instanceId = randomUUID();
    final var existedLinks = Collections.<InstanceAuthorityLink>emptyList();
    final var incomingLinks = links(instanceId, Link.of(0, 0), Link.of(1, 1));

    when(instanceLinkRepository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());
    mockAuthorities(incomingLinks);

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(2)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1]);

    assertThat(deleteCaptor.getValue()).isEmpty();
  }

  @Test
  void updateLinks_positive_deleteAllLinks_whenIncomingIsEmpty() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId, Link.of(0, 0), Link.of(1, 1));
    final var incomingLinks = Collections.<InstanceAuthorityLink>emptyList();

    when(instanceLinkRepository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).isEmpty();

    assertThat(deleteCaptor.getValue()).hasSize(2)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1]);
  }

  @Test
  void updateLinks_positive_deleteAllExistedAndSaveAllIncomingLinks() {
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

    when(instanceLinkRepository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());
    mockAuthorities(incomingLinks);

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).hasSize(4)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);
  }

  @Test
  void updateLinks_positive_saveOnlyNewLinks() {
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

    when(instanceLinkRepository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());
    mockAuthorities(incomingLinks);

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).isEmpty();
  }

  @Test
  void updateLinks_positive_deleteAndSaveLinks_whenHaveDifference() {
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

    when(instanceLinkRepository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());
    mockAuthorities(incomingLinks);

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).hasSize(2)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[2], Link.TAGS[3]);
  }

  @Test
  void countLinksByAuthorityIds_positive() {
    var authorityId1 = randomUUID();
    var authorityId2 = randomUUID();
    var authorityId3 = randomUUID();
    var resultSet = List.of(
      linkCountView(authorityId1, 10),
      linkCountView(authorityId2, 15)
    );

    when(instanceLinkRepository.countLinksByAuthorityIds(anySet())).thenReturn(resultSet);

    var authorityIds = Set.of(authorityId1, authorityId2, authorityId3);
    var result = service.countLinksByAuthorityIds(authorityIds);

    assertThat(result)
      .hasSize(2)
      .contains(entry(authorityId1, 10), entry(authorityId2, 15));
  }

  @Test
  void deleteByAuthorityIdIn_positive() {
    var authorityId = randomUUID();
    var authorityIds = Set.of(authorityId);

    service.deleteByAuthorityIdIn(authorityIds);

    verify(instanceLinkRepository).deleteByAuthorityIds(authorityIds);
  }

  @Test
  void saveAll_positive() {
    var instanceId = UUID.randomUUID();
    var links = links(2);

    service.saveAll(instanceId, links);

    verify(instanceLinkRepository).saveAll(links);
  }

  @Test
  @SuppressWarnings("unchecked")
  void getLinks_positive() {
    var status = LinkStatus.ACTUAL;
    var fromDate = OffsetDateTime.now();
    var toDate = fromDate.plus(1, ChronoUnit.DAYS);
    var limit = 1;
    var pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("updatedAt")));
    var expectedLinks = singletonList(InstanceAuthorityLink.builder()
      .id(1L)
      .build());

    when(instanceLinkRepository.findAll(any(Specification.class), eq(pageable)))
      .thenReturn(new PageImpl<>(expectedLinks, pageable, 0));

    var links = service.getLinks(status, fromDate, toDate, limit);

    assertThat(links)
      .isEqualTo(expectedLinks);
  }

  private ArgumentCaptor<List<InstanceAuthorityLink>> linksCaptor() {
    @SuppressWarnings("unchecked") var listClass = (Class<List<InstanceAuthorityLink>>) (Class<?>) List.class;
    return ArgumentCaptor.forClass(listClass);
  }

  private LinkCountView linkCountView(UUID id, int totalLinks) {
    return new LinkCountView() {
      @Override
      public UUID getId() {
        return id;
      }

      @Override
      public Integer getTotalLinks() {
        return totalLinks;
      }
    };
  }

  private void mockAuthorities(List<InstanceAuthorityLink> links) {
    var authoritiesById = links.stream()
        .map(InstanceAuthorityLink::getAuthority)
        .collect(Collectors.toMap(Authority::getId, Function.identity()));

    when(authorityService.getAllByIds(anyCollection())).thenReturn(authoritiesById);
  }

}
