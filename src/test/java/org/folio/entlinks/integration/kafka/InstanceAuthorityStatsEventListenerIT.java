package org.folio.entlinks.integration.kafka;

import static java.util.Collections.singletonList;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.folio.entlinks.domain.dto.LinkUpdateReport.StatusEnum.FAIL;
import static org.folio.support.TestUtils.Link.TAGS;
import static org.folio.support.TestUtils.linksDto;
import static org.folio.support.TestUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.instanceAuthorityStatsTopic;
import static org.folio.support.base.TestConstants.inventoryAuthorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ThreadUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.SoftAssertions;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.domain.entity.AuthorityDataStatStatus;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.domain.repository.AuthorityDataStatRepository;
import org.folio.entlinks.domain.repository.InstanceLinkRepository;
import org.folio.entlinks.utils.KafkaUtils;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.spring.tools.systemuser.SystemUserScopedExecutionService;
import org.folio.support.TestUtils;
import org.folio.support.TestUtils.Link;
import org.folio.support.base.IntegrationTestBase;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@IntegrationTest
@DatabaseCleanup(tables = {"instance_authority_link", "authority_data_stat"})
class InstanceAuthorityStatsEventListenerIT extends IntegrationTestBase {

  private KafkaMessageListenerContainer<String, LinksChangeEvent> container;
  private BlockingQueue<ConsumerRecord<String, LinksChangeEvent>> consumerRecords;

  @Autowired
  private SystemUserScopedExecutionService executionService;
  @Autowired
  private AuthorityDataStatRepository dataStatRepository;
  @Autowired
  private InstanceLinkRepository linkRepository;
  @Autowired
  private KafkaProperties kafkaProperties;

  private final Timestamp testStartTime = new Timestamp(System.currentTimeMillis());

  @BeforeEach
  void setUp() {
    consumerRecords = new LinkedBlockingQueue<>();

    var deserializer = new JsonDeserializer<>(LinksChangeEvent.class);
    kafkaProperties.getConsumer().setGroupId("test-group");
    Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
    config.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);

    DefaultKafkaConsumerFactory<String, LinksChangeEvent> consumer =
      new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);

    var topicName = KafkaUtils.getTenantTopicName("links.instance-authority", TENANT_ID);
    ContainerProperties containerProperties = new ContainerProperties(topicName);
    container = new KafkaMessageListenerContainer<>(consumer, containerProperties);
    container.setupMessageListener((MessageListener<String, LinksChangeEvent>) record -> consumerRecords.add(record));
    container.start();
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

    // save link
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId, link)), instanceId);
    // prepare and send inventory update authority event to save stats data
    var authUpdateEvent = TestUtils.authorityEvent("UPDATE",
      new AuthorityInventoryRecord().id(authorityId).personalName("new personal name").naturalId("naturalId"),
      new AuthorityInventoryRecord().id(authorityId).personalName("personal name").naturalId("naturalId"));
    sendKafkaMessage(inventoryAuthorityTopic(), authorityId.toString(), authUpdateEvent);

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
    sendKafkaMessage(instanceAuthorityStatsTopic(), event.getJobId().toString(), event);

    ThreadUtils.sleep(Duration.ofSeconds(2));

    // todo: replace scoped repository query with api call (when implemented in MODELINKS-34, MODELINKS-35)
    var updatedLink = executionService.executeSystemUserScoped(TENANT_ID,
      () -> linkRepository.findById(linkId).orElseThrow());
    var dataStat = executionService.executeSystemUserScoped(TENANT_ID,
      () -> dataStatRepository.findById(event.getJobId()).orElseThrow());

    var softAssertions = new SoftAssertions();

    // assert link updated
    softAssertions.assertThat(updatedLink.getStatus())
      .isEqualTo(InstanceAuthorityLinkStatus.ERROR);
    softAssertions.assertThat(updatedLink.getErrorCause())
      .isEqualTo(failCause);

    //assert authority data stat updated
    softAssertions.assertThat(dataStat.getLbUpdated())
      .isEqualTo(0);
    softAssertions.assertThat(dataStat.getLbFailed())
      .isEqualTo(1);
    softAssertions.assertThat(dataStat.getFailCause())
      .isBlank();
    softAssertions.assertThat(dataStat.getStatus())
      .isEqualTo(AuthorityDataStatStatus.FAILED);
    softAssertions.assertThat(dataStat.getCompletedAt())
      .isAfter(testStartTime);

    softAssertions.assertAll();
  }

  @Nullable
  @SneakyThrows
  private ConsumerRecord<String, LinksChangeEvent> getReceivedEvent() {
    return consumerRecords.poll(10, TimeUnit.SECONDS);
  }
}
