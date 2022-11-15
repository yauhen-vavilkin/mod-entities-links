package org.folio.entlinks.service;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.folio.support.TestUtils.links;
import static org.folio.support.TestUtils.linksDto;
import static org.folio.support.TestUtils.linksDtoCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.model.converter.InstanceLinkMapperImpl;
import org.folio.entlinks.model.entity.InstanceLink;
import org.folio.entlinks.model.projection.LinkCountView;
import org.folio.entlinks.repository.InstanceLinkRepository;
import org.folio.qm.domain.dto.InstanceLinkDto;
import org.folio.qm.domain.dto.LinksCountDto;
import org.folio.qm.domain.dto.UuidCollection;
import org.folio.spring.test.type.UnitTest;
import org.folio.support.TestUtils.Link;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceLinkServiceTest {

  @Mock
  private InstanceLinkRepository repository;

  private InstanceLinkService service;

  @BeforeEach
  void setUp() {
    service = new InstanceLinkService(repository, new InstanceLinkMapperImpl());
  }

  @Test
  void getInstanceLinks_positive_foundWhenExist() {
    var instanceId = randomUUID();
    var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    );

    when(repository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);

    var result = service.getInstanceLinks(instanceId);

    assertThat(result.getLinks())
      .hasSize(existedLinks.size())
      .extracting(InstanceLinkDto::getBibRecordTag)
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);
  }

  @Test
  void getInstanceLinks_positive_nothingFound() {
    var instanceId = randomUUID();
    var existedLinks = Collections.<InstanceLink>emptyList();

    when(repository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);

    var result = service.getInstanceLinks(instanceId);

    assertThat(result.getLinks()).isEmpty();
    assertEquals(0, result.getTotalRecords());
  }

  @Test
  void updateInstanceLinks_positive_saveIncomingLinks_whenAnyExist() {
    final var instanceId = randomUUID();
    final var existedLinks = Collections.<InstanceLink>emptyList();
    final var incomingLinks = linksDtoCollection(linksDto(instanceId, Link.of(0, 0), Link.of(1, 1)));

    when(repository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);
    doNothing().when(repository).deleteAllInBatch(any());
    when(repository.saveAll(any())).thenReturn(emptyList());

    service.updateInstanceLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(repository).saveAll(saveCaptor.capture());
    verify(repository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(2)
      .extracting(InstanceLink::getBibRecordTag)
      .containsOnly(Link.TAGS[0], Link.TAGS[1]);

    assertThat(deleteCaptor.getValue()).isEmpty();
  }

  @Test
  void updateInstanceLinks_positive_deleteAllLinks_whenIncomingIsEmpty() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId, Link.of(0, 0), Link.of(1, 1));
    final var incomingLinks = linksDtoCollection(emptyList());

    when(repository.findByInstanceId(any(UUID.class))).thenReturn(existedLinks);
    doNothing().when(repository).deleteAllInBatch(any());
    when(repository.saveAll(any())).thenReturn(emptyList());

    service.updateInstanceLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(repository).saveAll(saveCaptor.capture());
    verify(repository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).isEmpty();

    assertThat(deleteCaptor.getValue()).hasSize(2)
      .extracting(InstanceLink::getBibRecordTag)
      .containsOnly(Link.TAGS[0], Link.TAGS[1]);
  }

  @Test
  void updateInstanceLinks_positive_deleteAllExistedAndSaveAllIncomingLinks() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    );
    final var incomingLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 1),
      Link.of(1, 0),
      Link.of(2, 3),
      Link.of(3, 2)
    ));

    when(repository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(repository).deleteAllInBatch(any());
    when(repository.saveAll(any())).thenReturn(emptyList());

    service.updateInstanceLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(repository).saveAll(saveCaptor.capture());
    verify(repository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(InstanceLink::getBibRecordTag)
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).hasSize(4)
      .extracting(InstanceLink::getBibRecordTag)
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);
  }

  @Test
  void updateInstanceLinks_positive_saveOnlyNewLinks() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1)
    );
    final var incomingLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    ));

    when(repository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(repository).deleteAllInBatch(any());
    when(repository.saveAll(any())).thenReturn(emptyList());

    service.updateInstanceLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(repository).saveAll(saveCaptor.capture());
    verify(repository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(InstanceLink::getBibRecordTag)
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).isEmpty();
  }

  @Test
  void updateInstanceLinks_positive_deleteAndSaveLinks_whenHaveDifference() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    );
    final var incomingLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    ));

    when(repository.findByInstanceId(instanceId)).thenReturn(existedLinks);
    doNothing().when(repository).deleteAllInBatch(any());
    when(repository.saveAll(any())).thenReturn(emptyList());

    service.updateInstanceLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(repository).saveAll(saveCaptor.capture());
    verify(repository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(InstanceLink::getBibRecordTag)
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).hasSize(2)
      .extracting(InstanceLink::getBibRecordTag)
      .containsOnly(Link.TAGS[2], Link.TAGS[3]);
  }

  @Test
  void updateInstanceLinks_negative_whenInstanceIdIsNotSameForIncomingLinks() {
    var instanceId = randomUUID();
    var incomingLinks = linksDtoCollection(linksDto(randomUUID(),
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    ));

    var exception = Assertions.assertThrows(RequestBodyValidationException.class,
      () -> service.updateInstanceLinks(instanceId, incomingLinks));

    assertThat(exception)
      .hasMessage("Link should have instanceId = " + instanceId)
      .extracting(RequestBodyValidationException::getInvalidParameters)
      .returns(4, from(List::size));
  }

  @Test
  void countNumberOfTitles_positive_whenSomeIdsNotFoundThenFillInThemWithZeros() {
    var authorityId1 = randomUUID();
    var authorityId2 = randomUUID();
    var authorityId3 = randomUUID();
    var resultSet = List.of(
      linkCountView(authorityId1, 10),
      linkCountView(authorityId2, 15));
    when(repository.countLinksByAuthorityIds(anyList())).thenReturn(resultSet);

    var requestBody = new UuidCollection().ids(List.of(authorityId1, authorityId2, authorityId3));
    var result = service.countLinksByAuthorityIds(requestBody);

    assertThat(result.getLinks()).hasSize(3);
    assertThat(result.getLinks()).contains(
      new LinksCountDto().id(authorityId1).totalLinks(10L),
      new LinksCountDto().id(authorityId2).totalLinks(15L),
      new LinksCountDto().id(authorityId3).totalLinks(0L));
  }

  private ArgumentCaptor<List<InstanceLink>> linksCaptor() {
    @SuppressWarnings("unchecked") var listClass = (Class<List<InstanceLink>>) (Class<?>) List.class;
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
