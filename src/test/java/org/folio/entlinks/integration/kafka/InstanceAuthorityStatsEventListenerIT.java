package org.folio.entlinks.integration.kafka;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.folio.entlinks.domain.dto.LinkUpdateReport.StatusEnum.FAIL;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.Link.TAGS;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.authorityStatsEndpoint;
import static org.folio.support.base.TestConstants.inventoryAuthorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceAuthorityStatsTopic;
import static org.folio.support.base.TestConstants.linksInstanceAuthorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.folio.support.base.TestConstants.linksStatsInstanceEndpoint;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ThreadUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.LinkAction;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.TestDataUtils.Link;
import org.folio.support.base.IntegrationTestBase;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

@IntegrationTest
@DatabaseCleanup(tables = {DatabaseHelper.INSTANCE_AUTHORITY_LINK_TABLE,
                           DatabaseHelper.AUTHORITY_DATA_STAT_TABLE,
                           DatabaseHelper.AUTHORITY_DATA_TABLE})
class InstanceAuthorityStatsEventListenerIT extends IntegrationTestBase {

  private KafkaMessageListenerContainer<String, LinksChangeEvent> container;
  private BlockingQueue<ConsumerRecord<String, LinksChangeEvent>> consumerRecords;

  @BeforeEach
  void setUp(@Autowired KafkaProperties kafkaProperties) {
    consumerRecords = new LinkedBlockingQueue<>();
    container = createAndStartTestConsumer(linksInstanceAuthorityTopic(), consumerRecords, kafkaProperties,
      LinksChangeEvent.class);
  }

  @AfterEach
  void tearDown() {
    container.stop();
  }

  @Test
  @SneakyThrows
  void shouldHandleEvent_positive() {
    var instanceId = UUID.randomUUID();
    var authorityId = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f325e");
    var link = new Link(authorityId, TAGS[0]);

    prepareData(instanceId, authorityId, link);

    var linksChangeEvent = Objects.requireNonNull(getReceivedEvent()).value();

    // prepare and send instance authority stats event
    var linkId = linksChangeEvent.getUpdateTargets().get(0).getLinks().get(0).getLinkId();
    var failCause = "test";
    var event = new LinkUpdateReport()
      .tenant(TENANT_ID)
      .jobId(linksChangeEvent.getJobId())
      .instanceId(instanceId)
      .status(FAIL)
      .linkIds(singletonList(linkId.intValue()))
      .failCause(failCause);
    sendKafkaMessage(linksInstanceAuthorityStatsTopic(), event.getJobId().toString(), event);

    assertLinksUpdated(failCause);
  }

  @Test
  @SneakyThrows
  void shouldHandleEvent_positive_whenLinkIdsAndInstanceIdAreEmpty() {
    var instanceId = UUID.randomUUID();
    var authorityId = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f325e");
    var link = new Link(authorityId, TAGS[0]);

    // save link
    prepareData(instanceId, authorityId, link);

    var linksChangeEvent = Objects.requireNonNull(getReceivedEvent()).value();

    // prepare and send instance authority stats event
    var failCause = "test";
    var event = new LinkUpdateReport()
      .tenant(TENANT_ID)
      .jobId(linksChangeEvent.getJobId())
      .instanceId(null)
      .status(FAIL)
      .linkIds(emptyList())
      .failCause(failCause);
    sendKafkaMessage(linksInstanceAuthorityStatsTopic(), event.getJobId().toString(), event);

    assertLinksUpdated(failCause);
  }

  private void prepareData(UUID instanceId, UUID authorityId, Link link) {
    // save link
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId, link)), instanceId);
    // prepare and send inventory update authority event to save stats data
    var authUpdateEvent = TestDataUtils.authorityEvent("UPDATE",
      new AuthorityInventoryRecord().id(authorityId).personalName("new personal name").naturalId("naturalId"),
      new AuthorityInventoryRecord().id(authorityId).personalName("personal name").naturalId("naturalId"));
    sendKafkaMessage(inventoryAuthorityTopic(), authorityId.toString(), authUpdateEvent);
  }

  @SneakyThrows
  private void assertLinksUpdated(String failCause) {
    ThreadUtils.sleep(Duration.ofSeconds(2));

    doGet(linksStatsInstanceEndpoint(LinkStatus.ERROR, OffsetDateTime.now().minusDays(1), OffsetDateTime.now()))
      .andExpect(status().is2xxSuccessful())
      .andExpect(jsonPath("$.stats[0].errorCause", is(failCause)));

    doGet(authorityStatsEndpoint(
      LinkAction.UPDATE_HEADING, OffsetDateTime.now().minusDays(1), OffsetDateTime.now(), 1))
      .andExpect(status().is2xxSuccessful())
      .andExpect(jsonPath("$.stats[0].lbFailed", is(1)))
      .andExpect(jsonPath("$.stats[0].lbFailed", is(1)))
      .andExpect(jsonPath("$.stats[0].lbUpdated", is(0)));
  }

  @Nullable
  @SneakyThrows
  private ConsumerRecord<String, LinksChangeEvent> getReceivedEvent() {
    return consumerRecords.poll(10, TimeUnit.SECONDS);
  }
}
