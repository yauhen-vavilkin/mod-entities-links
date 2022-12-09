package org.folio.entlinks.controller.delegate;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.assertj.core.api.Assertions.tuple;
import static org.folio.support.TestUtils.links;
import static org.folio.support.TestUtils.linksDto;
import static org.folio.support.TestUtils.linksDtoCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.controller.converter.InstanceAuthorityLinkMapper;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.dto.LinksCountDto;
import org.folio.entlinks.domain.dto.UuidCollection;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.spring.test.type.UnitTest;
import org.folio.support.TestUtils;
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

  private @InjectMocks LinkingServiceDelegate delegate;

  @Test
  void getLinks_positive() {
    var linkData = TestUtils.Link.of(0, 0);
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
  void updateLinks_positive() {
    final var links = links(INSTANCE_ID,
      TestUtils.Link.of(0, 0),
      TestUtils.Link.of(1, 1),
      TestUtils.Link.of(2, 2),
      TestUtils.Link.of(3, 3)
    );
    final var dtoCollection = linksDtoCollection(linksDto(INSTANCE_ID,
      TestUtils.Link.of(0, 0),
      TestUtils.Link.of(1, 1),
      TestUtils.Link.of(2, 3),
      TestUtils.Link.of(3, 2)
    ));

    doNothing().when(linkingService).updateLinks(INSTANCE_ID, links);
    when(mapper.convertDto(dtoCollection.getLinks())).thenReturn(links);

    delegate.updateLinks(INSTANCE_ID, dtoCollection);

    verify(linkingService).updateLinks(INSTANCE_ID, links);
  }

  @Test
  void updateLinks_negative_whenInstanceIdIsNotSameForIncomingLinks() {
    var incomingLinks = linksDtoCollection(linksDto(randomUUID(),
      TestUtils.Link.of(0, 0),
      TestUtils.Link.of(1, 1),
      TestUtils.Link.of(2, 3),
      TestUtils.Link.of(3, 2)
    ));

    var exception = Assertions.assertThrows(RequestBodyValidationException.class,
      () -> delegate.updateLinks(INSTANCE_ID, incomingLinks));

    assertThat(exception)
      .hasMessage("Link should have instanceId = " + INSTANCE_ID)
      .extracting(RequestBodyValidationException::getInvalidParameters)
      .returns(4, from(List::size));
  }

  @Test
  void updateLinks_negative_whenInvalidSubfieldCodes() {
    var incomingLinks = linksDtoCollection(linksDto(INSTANCE_ID,
      TestUtils.Link.of(0, 2),
      TestUtils.Link.of(3, 2)
    ));
    incomingLinks.getLinks().get(0).addBibRecordSubfieldsItem("abc");
    incomingLinks.getLinks().get(1).addBibRecordSubfieldsItem("12");

    var exception = Assertions.assertThrows(RequestBodyValidationException.class,
      () -> delegate.updateLinks(INSTANCE_ID, incomingLinks));

    assertThat(exception)
      .hasMessage("Max Bib record subfield length is 1")
      .extracting(RequestBodyValidationException::getInvalidParameters)
      .returns(2, from(List::size));
  }

  @Test
  void countLinksByAuthorityIds_positive() {
    var ids = List.of(randomUUID(), randomUUID(), randomUUID());

    when(linkingService.countLinksByAuthorityIds(new HashSet<>(ids))).thenReturn(
      Map.of(ids.get(0), 2L, ids.get(1), 1L));
    when(mapper.convert(anyMap())).thenCallRealMethod();

    var actual = delegate.countLinksByAuthorityIds(new UuidCollection().ids(ids));

    assertThat(actual.getLinks())
      .hasSize(ids.size())
      .extracting(LinksCountDto::getId, LinksCountDto::getTotalLinks)
      .containsExactlyInAnyOrder(tuple(ids.get(0), 2L), tuple(ids.get(1), 1L), tuple(ids.get(2), 0L));
  }
}
