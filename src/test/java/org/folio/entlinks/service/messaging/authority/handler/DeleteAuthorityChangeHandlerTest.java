package org.folio.entlinks.service.messaging.authority.handler;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.folio.entlinks.domain.dto.LinksChangeEvent.TypeEnum;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.config.properties.InstanceAuthorityChangeProperties;
import org.folio.entlinks.domain.dto.ChangeTarget;
import org.folio.entlinks.domain.dto.ChangeTargetLink;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.integration.dto.event.AuthorityDomainEvent;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeHolder;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
import org.folio.spring.testing.type.UnitTest;
import org.folio.support.TestDataUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DeleteAuthorityChangeHandlerTest {

  private @Mock InstanceAuthorityLinkingService linkingService;
  private @Mock InstanceAuthorityChangeProperties properties;
  private @Mock AuthorityService authorityService;
  private @InjectMocks DeleteAuthorityChangeHandler handler;

  @Test
  void getReplyEventType_positive() {
    var actual = handler.getReplyEventType();

    assertEquals(TypeEnum.DELETE, actual);
  }

  @Test
  void supportedInventoryEventType_positive() {
    var actual = handler.supportedAuthorityChangeType();

    assertEquals(AuthorityChangeType.DELETE, actual);
  }

  @Test
  void handle_positive() {
    var eventIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
    var events = eventIds.stream()
      .map(uuid -> new AuthorityChangeHolder(new AuthorityDomainEvent(uuid), emptyMap(), emptyMap(), 1))
      .toList();
    var instanceId1 = UUID.randomUUID();
    var instanceId2 = UUID.randomUUID();
    var instanceId3 = UUID.randomUUID();
    var link1 = TestDataUtils.Link.of(1, 1);
    var link2 = TestDataUtils.Link.of(0, 2);
    var link3 = TestDataUtils.Link.of(2, 1);

    doNothing().when(linkingService).deleteByAuthorityIdIn(anySet());
    when(properties.getNumPartitions()).thenReturn(1);
    when(linkingService.getLinksByAuthorityId(eq(events.get(0).getAuthorityId()), any())).thenReturn(
      new PageImpl<>(List.of(link1.toEntity(instanceId1)), Pageable.ofSize(1), 2)
    ).thenReturn(
      new PageImpl<>(List.of(link2.toEntity(instanceId2)))
    );
    when(linkingService.getLinksByAuthorityId(eq(events.get(1).getAuthorityId()), any())).thenReturn(
      new PageImpl<>(List.of(link3.toEntity(instanceId3)))
    );

    var actual = handler.handle(events);

    verify(linkingService).deleteByAuthorityIdIn(eventIds);

    assertThat(actual)
      .hasSize(3)
      .extracting(LinksChangeEvent::getAuthorityId, LinksChangeEvent::getType, LinksChangeEvent::getUpdateTargets)
      .contains(
        tuple(events.get(0).getAuthorityId(), TypeEnum.DELETE, List.of(changeTarget(instanceId1, link1))),
        tuple(events.get(0).getAuthorityId(), TypeEnum.DELETE, List.of(changeTarget(instanceId2, link2))),
        tuple(events.get(1).getAuthorityId(), TypeEnum.DELETE, List.of(changeTarget(instanceId3, link3)))
      );
    verify(authorityService).batchDeleteByIds(anyCollection());
  }

  @Test
  void handle_positive_emptyEventList() {
    var actual = handler.handle(emptyList());

    assertThat(actual).isEmpty();
  }

  @Test
  void handle_positive_nullEventList() {
    var actual = handler.handle(null);

    assertThat(actual).isEmpty();
  }

  private ChangeTarget changeTarget(UUID instanceId, TestDataUtils.Link link) {
    return new ChangeTarget().field(link.tag()).links(
      Collections.singletonList(new ChangeTargetLink().instanceId(instanceId)));
  }
}
