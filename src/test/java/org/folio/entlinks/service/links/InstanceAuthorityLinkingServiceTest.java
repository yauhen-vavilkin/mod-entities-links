package org.folio.entlinks.service.links;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.folio.support.TestUtils.links;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.projection.LinkCountView;
import org.folio.entlinks.repository.InstanceLinkRepository;
import org.folio.spring.test.type.UnitTest;
import org.folio.support.TestUtils.Link;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceAuthorityLinkingServiceTest {

  @Mock
  private InstanceLinkRepository repository;

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

    when(repository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);

    var result = service.getLinksByInstanceId(instanceId);

    assertThat(result)
      .hasSize(existedLinks.size())
      .extracting(InstanceAuthorityLink::getBibRecordTag)
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);
  }

  @Test
  void getLinksByInstanceId_positive_nothingFound() {
    var instanceId = randomUUID();
    var existedLinks = Collections.<InstanceAuthorityLink>emptyList();

    when(repository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);

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

    when(repository.findByAuthorityId(any(UUID.class), any(Pageable.class))).thenReturn(linkPage);

    var result = service.getLinksByAuthorityId(UUID.randomUUID(), Pageable.ofSize(2));

    assertThat(result)
      .hasSize(links.size())
      .extracting(InstanceAuthorityLink::getBibRecordTag)
      .containsOnly(Link.TAGS[0], Link.TAGS[1]);
  }

  @Test
  void updateLinks_positive_saveIncomingLinks_whenAnyExist() {
    final var instanceId = randomUUID();
    final var existedLinks = Collections.<InstanceAuthorityLink>emptyList();
    final var incomingLinks = links(instanceId, Link.of(0, 0), Link.of(1, 1));

    when(repository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);
    doNothing().when(repository).deleteAllInBatch(any());
    when(repository.saveAll(any())).thenReturn(emptyList());

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(repository).saveAll(saveCaptor.capture());
    verify(repository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(2)
      .extracting(InstanceAuthorityLink::getBibRecordTag)
      .containsOnly(Link.TAGS[0], Link.TAGS[1]);

    assertThat(deleteCaptor.getValue()).isEmpty();
  }

  @Test
  void updateLinks_positive_deleteAllLinks_whenIncomingIsEmpty() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId, Link.of(0, 0), Link.of(1, 1));
    final var incomingLinks = Collections.<InstanceAuthorityLink>emptyList();

    when(repository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);
    doNothing().when(repository).deleteAllInBatch(any());
    when(repository.saveAll(any())).thenReturn(emptyList());

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(repository).saveAll(saveCaptor.capture());
    verify(repository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).isEmpty();

    assertThat(deleteCaptor.getValue()).hasSize(2)
      .extracting(InstanceAuthorityLink::getBibRecordTag)
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

    when(repository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(repository).deleteAllInBatch(any());
    when(repository.saveAll(any())).thenReturn(emptyList());

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(repository).saveAll(saveCaptor.capture());
    verify(repository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(InstanceAuthorityLink::getBibRecordTag)
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).hasSize(4)
      .extracting(InstanceAuthorityLink::getBibRecordTag)
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

    when(repository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(repository).deleteAllInBatch(any());
    when(repository.saveAll(any())).thenReturn(emptyList());

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(repository).saveAll(saveCaptor.capture());
    verify(repository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(InstanceAuthorityLink::getBibRecordTag)
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

    when(repository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(repository).deleteAllInBatch(any());
    when(repository.saveAll(any())).thenReturn(emptyList());

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(repository).saveAll(saveCaptor.capture());
    verify(repository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(InstanceAuthorityLink::getBibRecordTag)
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).hasSize(2)
      .extracting(InstanceAuthorityLink::getBibRecordTag)
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

    when(repository.countLinksByAuthorityIds(anySet())).thenReturn(resultSet);

    var authorityIds = Set.of(authorityId1, authorityId2, authorityId3);
    var result = service.countLinksByAuthorityIds(authorityIds);

    assertThat(result)
      .hasSize(2)
      .contains(entry(authorityId1, 10L), entry(authorityId2, 15L));
  }

  @Test
  void retainAuthoritiesIdsWithLinks_positive() {
    var authorityId1 = randomUUID();
    var authorityId2 = randomUUID();
    var authorityId3 = randomUUID();

    when(repository.findAuthorityIdsWithLinks(anySet())).thenReturn(Set.of(authorityId1, authorityId2));

    var authorityIds = Set.of(authorityId1, authorityId2, authorityId3);
    var result = service.retainAuthoritiesIdsWithLinks(authorityIds);

    assertThat(result)
      .hasSize(2)
      .contains(authorityId1, authorityId2);
  }

  @Test
  void updateNaturalId_positive() {
    var naturalId = "naturalId";
    var authorityId = randomUUID();

    service.updateNaturalId(naturalId, authorityId);

    verify(repository).updateNaturalId(naturalId, authorityId);
  }

  @Test
  void updateSubfieldsAndNaturalId_positive() {
    var subfields = new char[] {'a', 'b'};
    var naturalId = "naturalId";
    var authorityId = randomUUID();
    var tag = "100";

    service.updateSubfieldsAndNaturalId(subfields, naturalId, authorityId, tag);

    verify(repository).updateSubfieldsAndNaturalId(subfields, naturalId, authorityId, tag);
  }

  @Test
  void deleteByAuthorityIdIn_positive() {
    var authorityId = randomUUID();
    var authorityIds = Set.of(authorityId);

    service.deleteByAuthorityIdIn(authorityIds);

    verify(repository).deleteByAuthorityIdIn(authorityIds);
  }

  private ArgumentCaptor<List<InstanceAuthorityLink>> linksCaptor() {
    @SuppressWarnings("unchecked") var listClass = (Class<List<InstanceAuthorityLink>>) (Class<?>) List.class;
    return ArgumentCaptor.forClass(listClass);
  }

  private LinkCountView linkCountView(UUID id, long totalLinks) {
    return new LinkCountView() {
      @Override
      public UUID getId() {
        return id;
      }

      @Override
      public Long getTotalLinks() {
        return totalLinks;
      }
    };
  }

}
