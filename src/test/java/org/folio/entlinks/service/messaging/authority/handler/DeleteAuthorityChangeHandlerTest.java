package org.folio.entlinks.service.messaging.authority.handler;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.dto.InventoryEventType;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.spring.test.type.UnitTest;
import org.folio.support.TestUtils;
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
  private @InjectMocks DeleteAuthorityChangeHandler handler;

  @Test
  void getReplyEventType_positive() {
    var actual = handler.getReplyEventType();

    assertEquals(LinksChangeEvent.TypeEnum.DELETE, actual);
  }

  @Test
  void supportedInventoryEventType_positive() {
    var actual = handler.supportedInventoryEventType();

    assertEquals(InventoryEventType.DELETE, actual);
  }

  @Test
  void handle_positive() {
    var eventIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
    var events = eventIds.stream().map(uuid -> new InventoryEvent().id(uuid)).toList();
    var instanceId1 = UUID.randomUUID();
    var instanceId2 = UUID.randomUUID();
    var instanceId3 = UUID.randomUUID();
    var link1 = TestUtils.Link.of(1, 1);
    var link2 = TestUtils.Link.of(0, 2);
    var link3 = TestUtils.Link.of(2, 1);

    doNothing().when(linkingService).deleteByAuthorityIdIn(anySet());
    when(properties.getNumPartitions()).thenReturn(1);
    when(linkingService.getLinksByAuthorityId(eq(events.get(0).getId()), any())).thenReturn(
      new PageImpl<>(List.of(link1.toEntity(instanceId1)), Pageable.ofSize(1), 2)
    ).thenReturn(
      new PageImpl<>(List.of(link2.toEntity(instanceId2)))
    );
    when(linkingService.getLinksByAuthorityId(eq(events.get(1).getId()), any())).thenReturn(
      new PageImpl<>(List.of(link3.toEntity(instanceId3)))
    );

    var actual = handler.handle(events);

    verify(linkingService).deleteByAuthorityIdIn(eventIds);

    assertThat(actual)
      .hasSize(3)
      .extracting(LinksChangeEvent::getAuthorityId, LinksChangeEvent::getType, LinksChangeEvent::getUpdateTargets)
      .contains(
        tuple(events.get(0).getId(), LinksChangeEvent.TypeEnum.DELETE, List.of(changeTarget(instanceId1, link1))),
        tuple(events.get(0).getId(), LinksChangeEvent.TypeEnum.DELETE, List.of(changeTarget(instanceId2, link2))),
        tuple(events.get(1).getId(), LinksChangeEvent.TypeEnum.DELETE, List.of(changeTarget(instanceId3, link3)))
      );
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

  private ChangeTarget changeTarget(UUID instanceId, TestUtils.Link link) {
    return new ChangeTarget().field(link.tag()).instanceIds(Collections.singletonList(instanceId));
  }
}
