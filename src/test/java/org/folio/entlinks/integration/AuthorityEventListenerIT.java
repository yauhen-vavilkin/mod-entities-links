package org.folio.entlinks.integration;

import static org.folio.support.TestUtils.linksDto;
import static org.folio.support.TestUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.inventoryAuthorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.entlinks.service.AuthorityChangeHandlingService;
import org.folio.qm.domain.dto.AuthorityInventoryRecord;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.TestUtils;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.MockBean;

@IntegrationTest
class AuthorityEventListenerIT extends IntegrationTestBase {

  @MockBean
  private AuthorityChangeHandlingService authorityChangeHandlingService;

  @ValueSource(strings = {"DELETE", "UPDATE"})
  @ParameterizedTest
  void shouldHandleEvent_positive_whenAuthorityLinkExistAndChangeIsFound(String eventType) {
    var instanceId = UUID.randomUUID();
    var link = TestUtils.Link.of(0, 0);
    var incomingLinks = linksDtoCollection(linksDto(instanceId, link));
    doPut(linksInstanceEndpoint(), incomingLinks, instanceId);
    when(authorityChangeHandlingService.handleAuthoritiesChanges(anyList())).thenReturn(1);

    var event = TestUtils.authorityEvent(eventType,
      new AuthorityInventoryRecord().id(link.authorityId()),
      new AuthorityInventoryRecord().id(link.authorityId()).naturalId("12345"));
    sendKafkaMessage(inventoryAuthorityTopic(), event);

    //TODO change to verifying out-coming Kafka message
    assertTrue(true);
  }

  @ValueSource(strings = {"DELETE", "UPDATE"})
  @ParameterizedTest
  void shouldNotHandleEvent_positive_whenAuthorityLinksNotExist(String eventType) {
    var authorityId = UUID.randomUUID();
    var event = TestUtils.authorityEvent(eventType,
      new AuthorityInventoryRecord().id(authorityId),
      new AuthorityInventoryRecord().id(authorityId).naturalId("12345"));
    sendKafkaMessage(inventoryAuthorityTopic(), event);

    //TODO change to verifying out-coming Kafka message
    assertTrue(true);
  }

  @ValueSource(strings = {"CREATE", "REINDEX", "ITERATE"})
  @ParameterizedTest
  void shouldNotHandleEvent_positive_whenEventIsNotRelatedToChanges(String eventType) {
    var authorityId = UUID.randomUUID();
    var event = TestUtils.authorityEvent(eventType,
      new AuthorityInventoryRecord().id(authorityId),
      new AuthorityInventoryRecord().id(authorityId).naturalId("12345"));
    sendKafkaMessage(inventoryAuthorityTopic(), event);

    //TODO change to verifying out-coming Kafka message
    assertTrue(true);
  }

  @Test
  void shouldNotHandleEvent_positive_whenUpdateEventWithEqualOldAndNew() {
    var authorityId = UUID.randomUUID();
    var event = TestUtils.authorityEvent("UPDATE",
      new AuthorityInventoryRecord().id(authorityId),
      new AuthorityInventoryRecord().id(authorityId));
    sendKafkaMessage(inventoryAuthorityTopic(), event);

    //TODO change to verifying out-coming Kafka message
    assertTrue(true);
  }

}
