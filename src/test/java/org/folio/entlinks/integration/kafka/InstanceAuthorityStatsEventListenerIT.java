package org.folio.entlinks.integration.kafka;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.dto.LinkUpdateReport.StatusEnum.FAIL;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.authorityStatsEndpoint;
import static org.folio.support.base.TestConstants.inventoryAuthorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceAuthorityStatsTopic;
import static org.folio.support.base.TestConstants.linksInstanceAuthorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.folio.support.base.TestConstants.linksStatsInstanceEndpoint;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.AuthorityStatsDtoCollection;
import org.folio.entlinks.domain.dto.BibStatsDtoCollection;
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
  DatabaseHelper.AUTHORITY_TABLE})
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
    var authorityId = TestDataUtils.AUTHORITY_IDS[0];
    var link = Link.of(0, 1, TestDataUtils.NATURAL_IDS[0]);
    var authority = TestDataUtils.AuthorityTestData.authority(0, 0);
    databaseHelper.saveAuthority(TENANT_ID, authority);

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
    var authorityId = TestDataUtils.AUTHORITY_IDS[0];
    var link = Link.of(0, 1, TestDataUtils.NATURAL_IDS[0]);
    var authority = TestDataUtils.AuthorityTestData.authority(0, 0);
    databaseHelper.saveAuthority(TENANT_ID, authority);

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
      new AuthorityInventoryRecord().id(authorityId).personalName("personal name").naturalId("naturalId1"),
      new AuthorityInventoryRecord().id(authorityId).personalName("personal name").naturalId("naturalId2"));
    sendKafkaMessage(inventoryAuthorityTopic(), authorityId.toString(), authUpdateEvent);
  }

  @SneakyThrows
  private void assertLinksUpdated(String failCause) {
    var now = OffsetDateTime.now();
    awaitUntilAsserted(() -> {
      var content = doGet(linksStatsInstanceEndpoint(LinkStatus.ERROR, now.minusDays(1), now))
          .andReturn().getResponse().getContentAsString();
      var bibStatsDtoCollection = objectMapper.readValue(content, BibStatsDtoCollection.class);
      assertThat(bibStatsDtoCollection).isNotNull();
      assertThat(bibStatsDtoCollection.getStats()).hasSize(1);
      assertThat(bibStatsDtoCollection.getStats().get(0).getErrorCause()).isEqualTo(failCause);
    });

    awaitUntilAsserted(() -> {
      var content = doGet(authorityStatsEndpoint(
          LinkAction.UPDATE_NATURAL_ID, OffsetDateTime.now().minusDays(1), OffsetDateTime.now(), 1))
          .andReturn().getResponse().getContentAsString();
      var authorityStatsDtoCollection = objectMapper.readValue(content, AuthorityStatsDtoCollection.class);
      assertThat(authorityStatsDtoCollection).isNotNull();
      assertThat(authorityStatsDtoCollection.getStats()).hasSize(1);
      assertThat(authorityStatsDtoCollection.getStats().get(0).getLbFailed()).isOne();
      assertThat(authorityStatsDtoCollection.getStats().get(0).getLbUpdated()).isZero();
    });
  }

  @Nullable
  @SneakyThrows
  private ConsumerRecord<String, LinksChangeEvent> getReceivedEvent() {
    return consumerRecords.poll(10, TimeUnit.SECONDS);
  }
}
