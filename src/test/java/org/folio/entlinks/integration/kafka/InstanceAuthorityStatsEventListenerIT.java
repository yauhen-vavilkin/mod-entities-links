package org.folio.entlinks.integration.kafka;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.folio.entlinks.domain.dto.LinkUpdateReport.StatusEnum.FAIL;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.authorityStatsEndpoint;
import static org.folio.support.base.TestConstants.linksInstanceAuthorityStatsTopic;
import static org.folio.support.base.TestConstants.linksInstanceAuthorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.folio.support.base.TestConstants.linksStatsInstanceEndpoint;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Durations;
import org.folio.entlinks.domain.dto.LinkAction;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.TestDataUtils.Link;
import org.folio.support.base.IntegrationTestBase;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.INSTANCE_AUTHORITY_LINK_TABLE,
  DatabaseHelper.AUTHORITY_DATA_STAT_TABLE,
  DatabaseHelper.AUTHORITY_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE})
class InstanceAuthorityStatsEventListenerIT extends IntegrationTestBase {

  private KafkaMessageListenerContainer<String, LinksChangeEvent> container;
  private BlockingQueue<ConsumerRecord<String, LinksChangeEvent>> consumerRecords;

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

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
    prepareData(instanceId);

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
    prepareData(instanceId);

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

  private void prepareData(UUID instanceId) throws JsonProcessingException, UnsupportedEncodingException {
    var link = Link.of(0, 1, TestDataUtils.NATURAL_IDS[0]);
    var sourceFile = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, sourceFile);
    var authority = TestDataUtils.AuthorityTestData.authority(0, 0);
    databaseHelper.saveAuthority(TENANT_ID, authority);
    var authorityId = TestDataUtils.AUTHORITY_IDS[0];

    // save link
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId, link)), instanceId);

    // prepare and send update authority request to generate update event to save stats data
    var content = doGet(authorityEndpoint(authorityId)).andReturn().getResponse().getContentAsString();
    var authorityDto = objectMapper.readValue(content, org.folio.entlinks.domain.dto.AuthorityDto.class);
    authorityDto.setNaturalId(authority.getNaturalId() + " updated");
    doPut(authorityEndpoint(authorityId), authorityDto);
  }

  @SneakyThrows
  private void assertLinksUpdated(String failCause) {
    var now = OffsetDateTime.now();
    awaitUntilAsserted(() ->
        doGet(linksStatsInstanceEndpoint(LinkStatus.ERROR, now.minusDays(1), now))
            .andExpect(jsonPath("$.stats", hasSize(1)))
            .andExpect(jsonPath("$.stats[0].errorCause", is(failCause)))
    );

    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        doGet(authorityStatsEndpoint(LinkAction.UPDATE_NATURAL_ID, now.minusDays(1), now, 1))
            .andExpect(jsonPath("$.stats", hasSize(1)))
            .andExpect(jsonPath("$.stats[0].lbFailed", is(1)))
            .andExpect(jsonPath("$.stats[0].lbUpdated", is(0)))
    );
  }

  @Nullable
  @SneakyThrows
  private ConsumerRecord<String, LinksChangeEvent> getReceivedEvent() {
    return consumerRecords.poll(10, TimeUnit.SECONDS);
  }
}
