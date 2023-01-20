package org.folio.entlinks.integration.kafka;

import static java.util.Objects.requireNonNull;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.folio.support.TestUtils.linksDto;
import static org.folio.support.TestUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.inventoryAuthorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.BDDSoftAssertions;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.ChangeTarget;
import org.folio.entlinks.domain.dto.ChangeTargetLink;
import org.folio.entlinks.domain.dto.FieldChange;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.domain.dto.SubfieldChange;
import org.folio.entlinks.utils.KafkaUtils;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.TestUtils;
import org.folio.support.base.IntegrationTestBase;
import org.jetbrains.annotations.NotNull;
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
@DatabaseCleanup(tables = "instance_authority_link")
class AuthorityInventoryEventListenerIT extends IntegrationTestBase {

  private KafkaMessageListenerContainer<String, LinksChangeEvent> container;

  private BlockingQueue<ConsumerRecord<String, LinksChangeEvent>> consumerRecords;

  @Autowired
  private KafkaProperties kafkaProperties;

  @NotNull
  private static List<ChangeTarget> getChangeTargets(LinksChangeEvent value) {
    for (ChangeTarget changeTarget : value.getUpdateTargets()) {
      for (ChangeTargetLink l : changeTarget.getLinks()) {
        l.setLinkId(null);
      }
    }
    return value.getUpdateTargets();
  }

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

  @SneakyThrows
  @Test
  void shouldHandleDeleteEvent_positive_whenAuthorityLinkExists() {
    var instanceId1 = UUID.randomUUID();
    var instanceId2 = UUID.randomUUID();
    var instanceId3 = UUID.randomUUID();
    var link1 = TestUtils.Link.of(0, 0);
    var link2 = TestUtils.Link.of(0, 2);
    var link3 = TestUtils.Link.of(0, 0);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId1, link1)), instanceId1);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId2, link2)), instanceId2);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId3, link3)), instanceId3);

    var event = TestUtils.authorityEvent("DELETE", null,
      new AuthorityInventoryRecord().id(link1.authorityId()).naturalId("oldNaturalId"));
    sendKafkaMessage(inventoryAuthorityTopic(), link1.authorityId().toString(), event);

    var received = getReceivedEvent();

    // check sent event fields
    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();

    var headers = requireNonNull(received).headers();
    assertions.then(headers)
      .as("Headers")
      .extracting(Header::key)
      .contains(XOkapiHeaders.TENANT, XOkapiHeaders.URL, XOkapiHeaders.TOKEN);

    var value = received.value();
    assertions.then(value.getTenant()).as("Tenant").isEqualTo(TENANT_ID);
    assertions.then(value.getType()).as("Type").isEqualTo(LinksChangeEvent.TypeEnum.DELETE);
    assertions.then(value.getAuthorityId()).as("Authority ID").isEqualTo(link1.authorityId());
    assertions.then(getChangeTargets(value)).as("Update targets")
      .isEqualTo(List.of(
        updateTarget(link1.tag(), instanceId1, instanceId3),
        updateTarget(link2.tag(), instanceId2)
      ));
    assertions.then(value.getSubfieldsChanges()).as("Subfield changes").isEmpty();
    assertions.then(value.getJobId()).as("Job ID").isNotNull();
    assertions.then(value.getTs()).as("Timestamp").isNotNull();

    assertions.assertAll();

    // check that links were deleted
    doGet(linksInstanceEndpoint(), instanceId1)
      .andExpect(jsonPath("$.links", hasSize(0)));
    doGet(linksInstanceEndpoint(), instanceId2)
      .andExpect(jsonPath("$.links", hasSize(0)));
    doGet(linksInstanceEndpoint(), instanceId3)
      .andExpect(jsonPath("$.links", hasSize(0)));
  }

  @SneakyThrows
  @Test
  void shouldHandleUpdateEvent_positive_whenAuthorityLinkExistAndFieldChangedFromOneToAnother() {
    // prepare links
    var authorityId = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f325e");
    var instanceId1 = UUID.randomUUID();
    var instanceId2 = UUID.randomUUID();
    var instanceId3 = UUID.randomUUID();
    var link1 = new TestUtils.Link(authorityId, "100", "naturalId", new char[] {'a', 'b', 'c'});
    var link2 = new TestUtils.Link(authorityId, "240", "naturalId", new char[] {'a', 'b', 'c'});
    var link3 = new TestUtils.Link(authorityId, "100", "naturalId", new char[] {'a', 'b', 'c'});
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId1, link1)), instanceId1);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId2, link2)), instanceId2);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId3, link3)), instanceId3);

    // prepare and send inventory update authority event
    var event = TestUtils.authorityEvent("UPDATE",
      new AuthorityInventoryRecord().id(authorityId).personalName("new personal name").naturalId("naturalId"),
      new AuthorityInventoryRecord().id(authorityId).personalNameTitle("old").naturalId("naturalId"));
    sendKafkaMessage(inventoryAuthorityTopic(), authorityId.toString(), event);

    var received = getReceivedEvent();

    // check sent event fields
    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();

    var headers = requireNonNull(received).headers();
    assertions.then(headers)
      .as("Headers")
      .extracting(Header::key)
      .contains(XOkapiHeaders.TENANT, XOkapiHeaders.URL, XOkapiHeaders.TOKEN);

    var value = received.value();
    assertions.then(value.getTenant()).as("Tenant").isEqualTo(TENANT_ID);
    assertions.then(value.getType()).as("Type").isEqualTo(LinksChangeEvent.TypeEnum.DELETE);
    assertions.then(value.getAuthorityId()).as("Authority ID").isEqualTo(link1.authorityId());
    assertions.then(getChangeTargets(value)).as("Update targets")
      .isEqualTo(List.of(
        updateTarget(link1.tag(), instanceId1, instanceId3),
        updateTarget(link2.tag(), instanceId2)
      ));
    assertions.then(value.getSubfieldsChanges()).as("Subfield changes").isEmpty();
    assertions.then(value.getJobId()).as("Job ID").isNotNull();
    assertions.then(value.getTs()).as("Timestamp").isNotNull();

    assertions.assertAll();

    // check that links were deleted
    doGet(linksInstanceEndpoint(), instanceId1)
      .andExpect(jsonPath("$.links", hasSize(0)));
    doGet(linksInstanceEndpoint(), instanceId2)
      .andExpect(jsonPath("$.links", hasSize(0)));
    doGet(linksInstanceEndpoint(), instanceId3)
      .andExpect(jsonPath("$.links", hasSize(0)));
  }

  @SneakyThrows
  @Test
  void shouldHandleUpdateEvent_positive_whenAuthorityLinkExistAndOnlyNaturalIdChanged() {
    var instanceId1 = UUID.randomUUID();
    var instanceId2 = UUID.randomUUID();
    var instanceId3 = UUID.randomUUID();
    var link1 = TestUtils.Link.of(0, 0);
    var link2 = TestUtils.Link.of(0, 2);
    var link3 = TestUtils.Link.of(0, 0);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId1, link1)), instanceId1);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId2, link2)), instanceId2);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId3, link3)), instanceId3);

    var event = TestUtils.authorityEvent("UPDATE",
      new AuthorityInventoryRecord().id(link1.authorityId()).naturalId("newNaturalId")
        .sourceFileId(UUID.fromString("af045f2f-e851-4613-984c-4bc13430454a")),
      new AuthorityInventoryRecord().id(link1.authorityId()).naturalId("1"));
    sendKafkaMessage(inventoryAuthorityTopic(), link1.authorityId().toString(), event);

    var received = getReceivedEvent();

    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();

    var headers = requireNonNull(received).headers();
    assertions.then(headers)
      .as("Headers")
      .extracting(Header::key)
      .contains(XOkapiHeaders.TENANT, XOkapiHeaders.URL, XOkapiHeaders.TOKEN);

    var value = received.value();
    assertions.then(value.getTenant()).as("Tenant").isEqualTo(TENANT_ID);
    assertions.then(value.getType()).as("Type").isEqualTo(LinksChangeEvent.TypeEnum.UPDATE);
    assertions.then(value.getAuthorityId()).as("Authority ID").isEqualTo(link1.authorityId());
    assertions.then(getChangeTargets(value)).as("Update targets")
      .isEqualTo(List.of(
        updateTarget(link1.tag(), instanceId1, instanceId3),
        updateTarget(link2.tag(), instanceId2)
      ));
    assertions.then(value.getSubfieldsChanges()).as("Subfield changes")
      .isEqualTo(List.of(
        new FieldChange()
          .field(link1.tag()).subfields(List.of(new SubfieldChange().code("0")
            .value("id.loc.gov/authorities/names/newNaturalId"))),
        new FieldChange()
          .field(link2.tag()).subfields(List.of(new SubfieldChange().code("0")
            .value("id.loc.gov/authorities/names/newNaturalId")))));
    assertions.then(value.getJobId()).as("Job ID").isNotNull();
    assertions.then(value.getTs()).as("Timestamp").isNotNull();

    assertions.assertAll();

    // check that links were updated according to authority changes
    doGet(linksInstanceEndpoint(), instanceId1)
      .andExpect(jsonPath("$.links[0].authorityNaturalId", equalTo("newNaturalId")));
    doGet(linksInstanceEndpoint(), instanceId2)
      .andExpect(jsonPath("$.links[0].authorityNaturalId", equalTo("newNaturalId")));
    doGet(linksInstanceEndpoint(), instanceId3)
      .andExpect(jsonPath("$.links[0].authorityNaturalId", equalTo("newNaturalId")));
  }

  @SneakyThrows
  @Test
  void shouldHandleUpdateEvent_positive_whenAuthorityLinkExistAndHeadingChanged() {
    // prepare links
    var authorityId = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f325e");
    var instanceId1 = UUID.randomUUID();
    var instanceId2 = UUID.randomUUID();
    var instanceId3 = UUID.randomUUID();
    var link1 = new TestUtils.Link(authorityId, "100", "naturalId", new char[] {'a', 'b', 'c'});
    var link2 = new TestUtils.Link(authorityId, "240", "naturalId", new char[] {'a', 'b', 'c'});
    var link3 = new TestUtils.Link(authorityId, "100", "naturalId", new char[] {'a', 'b', 'c'});
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId1, link1)), instanceId1);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId2, link2)), instanceId2);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId3, link3)), instanceId3);

    // prepare and send inventory update authority event
    var event = TestUtils.authorityEvent("UPDATE",
      new AuthorityInventoryRecord().id(authorityId).personalName("new personal name").naturalId("naturalId")
        .sourceFileId(UUID.fromString("af045f2f-e851-4613-984c-4bc13430454a")),
      new AuthorityInventoryRecord().id(authorityId).personalName("old").naturalId("naturalId"));
    sendKafkaMessage(inventoryAuthorityTopic(), authorityId.toString(), event);

    var received = getReceivedEvent();

    // check sent event fields
    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();

    var headers = requireNonNull(received).headers();
    assertions.then(headers)
      .as("Headers")
      .extracting(Header::key)
      .contains(XOkapiHeaders.TENANT, XOkapiHeaders.URL, XOkapiHeaders.TOKEN);

    var value = received.value();
    assertions.then(value.getTenant()).as("Tenant").isEqualTo(TENANT_ID);
    assertions.then(value.getType()).as("Type").isEqualTo(LinksChangeEvent.TypeEnum.UPDATE);
    assertions.then(value.getAuthorityId()).as("Authority ID").isEqualTo(link1.authorityId());
    assertions.then(getChangeTargets(value)).as("Update targets")
      .isEqualTo(List.of(
        updateTarget(link1.tag(), instanceId1, instanceId3),
        updateTarget(link2.tag(), instanceId2)
      ));
    assertions.then(value.getSubfieldsChanges()).as("Subfield changes").containsExactlyInAnyOrder(
      new FieldChange().field("100").subfields(List.of(
        new SubfieldChange().code("a").value("Lansing, John"),
        new SubfieldChange().code("d").value("1756-1791."),
        new SubfieldChange().code("q").value("(Jules)")
      )),
      new FieldChange().field("600").subfields(List.of(
        new SubfieldChange().code("a").value("Lansing, John"),
        new SubfieldChange().code("d").value("1756-1791."),
        new SubfieldChange().code("l").value("book"),
        new SubfieldChange().code("q").value("(Jules)"),
        new SubfieldChange().code("t").value("Black Eagles")
      )),
      new FieldChange().field("700").subfields(List.of(
        new SubfieldChange().code("a").value("Lansing, John"),
        new SubfieldChange().code("d").value("1756-1791."),
        new SubfieldChange().code("l").value("book"),
        new SubfieldChange().code("q").value("(Jules)"),
        new SubfieldChange().code("t").value("Black Eagles")
      )),
      new FieldChange().field("800").subfields(List.of(
        new SubfieldChange().code("a").value("Lansing, John"),
        new SubfieldChange().code("d").value("1756-1791."),
        new SubfieldChange().code("l").value("book"),
        new SubfieldChange().code("q").value("(Jules)"),
        new SubfieldChange().code("t").value("Black Eagles")
      )),
      new FieldChange().field("240").subfields(List.of(
        new SubfieldChange().code("a").value("Black Eagles"),
        new SubfieldChange().code("l").value("book")
      ))
    );
    assertions.then(value.getJobId()).as("Job ID").isNotNull();
    assertions.then(value.getTs()).as("Timestamp").isNotNull();

    assertions.assertAll();

    // check that links were updated according to authority changes
    doGet(linksInstanceEndpoint(), instanceId1)
      .andExpect(jsonPath("$.links[0].bibRecordSubfields", containsInAnyOrder("a", "d", "q")));
    doGet(linksInstanceEndpoint(), instanceId2)
      .andExpect(jsonPath("$.links[0].bibRecordSubfields", containsInAnyOrder("a", "l")));
  }

  @Nullable
  @SneakyThrows
  private ConsumerRecord<String, LinksChangeEvent> getReceivedEvent() {
    return consumerRecords.poll(10, TimeUnit.SECONDS);
  }

  private ChangeTarget updateTarget(String tag, UUID... instanceIds) {
    return new ChangeTarget().field(tag).links(Arrays.asList(instanceIds).stream()
      .map(uuid -> new ChangeTargetLink().instanceId(uuid))
      .toList());
  }

}
