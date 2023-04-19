package org.folio.entlinks.integration.kafka;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.DELETE_TYPE;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.UPDATE_TYPE;
import static org.folio.support.base.TestConstants.inventoryAuthorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceAuthorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.assertj.core.api.BDDSoftAssertions;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.ChangeTarget;
import org.folio.entlinks.domain.dto.ChangeTargetLink;
import org.folio.entlinks.domain.dto.FieldChange;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.domain.dto.SubfieldChange;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.base.IntegrationTestBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

@IntegrationTest
@DatabaseCleanup(tables = DatabaseHelper.INSTANCE_AUTHORITY_LINK_TABLE)
class AuthorityInventoryEventListenerIT extends IntegrationTestBase {

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

  @SneakyThrows
  @Test
  void shouldHandleDeleteEvent_positive_whenAuthorityLinkExists() {
    var instanceId1 = UUID.randomUUID();
    var instanceId2 = UUID.randomUUID();
    var instanceId3 = UUID.randomUUID();
    var link1 = TestDataUtils.Link.of(0, 0);
    var link2 = TestDataUtils.Link.of(0, 2);
    var link3 = TestDataUtils.Link.of(0, 0);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId1, link1)), instanceId1);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId2, link2)), instanceId2);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId3, link3)), instanceId3);

    var event = TestDataUtils.authorityEvent(DELETE_TYPE, null,
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

    // checking authorityData state field for deleting
    var authorityData = databaseHelper.getAuthority(link1.authorityId());
    assertions.then(authorityData.getId()).as("Id").isEqualTo(link1.authorityId());
    assertions.then(authorityData.getId()).as("Natural Id").isEqualTo("oldNaturalId");
    assertions.then(authorityData.isDeleted()).as("State").isTrue();
  }

  @SneakyThrows
  @Test
  void shouldHandleUpdateEvent_positive_whenAuthorityLinkExistAndFieldChangedFromOneToAnother() {
    // prepare links
    var authorityId = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f325e");
    var instanceId1 = UUID.randomUUID();
    var instanceId2 = UUID.randomUUID();
    var instanceId3 = UUID.randomUUID();
    var link1 = new TestDataUtils.Link(authorityId, "100", "naturalId", new char[] {'a', 'b', 'c'});
    var link2 = new TestDataUtils.Link(authorityId, "240", "naturalId", new char[] {'a', 'b', 'c'});
    var link3 = new TestDataUtils.Link(authorityId, "100", "naturalId", new char[] {'a', 'b', 'c'});
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId1, link1)), instanceId1);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId2, link2)), instanceId2);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId3, link3)), instanceId3);

    // prepare and send inventory update authority event
    var event = TestDataUtils.authorityEvent(UPDATE_TYPE,
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
    var link1 = TestDataUtils.Link.of(0, 0);
    var link2 = TestDataUtils.Link.of(0, 2);
    var link3 = TestDataUtils.Link.of(0, 0);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId1, link1)), instanceId1);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId2, link2)), instanceId2);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId3, link3)), instanceId3);

    var event = TestDataUtils.authorityEvent(UPDATE_TYPE,
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
          .field(link1.tag()).subfields(List.of(subfieldChange("0", "id.loc.gov/authorities/names/newNaturalId"))),
        new FieldChange()
          .field(link2.tag()).subfields(List.of(subfieldChange("0", "id.loc.gov/authorities/names/newNaturalId")))));
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
    var link1 = new TestDataUtils.Link(authorityId, "100", "naturalId", new char[] {'a', 'b', 'c'});
    var link2 = new TestDataUtils.Link(authorityId, "240", "naturalId", new char[] {'a', 'b', 'c'});
    var link3 = new TestDataUtils.Link(authorityId, "100", "naturalId", new char[] {'a', 'b', 'c'});
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId1, link1)), instanceId1);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId2, link2)), instanceId2);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId3, link3)), instanceId3);

    // prepare and send inventory update authority event
    var event = TestDataUtils.authorityEvent(UPDATE_TYPE,
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
        subfieldChange("a", "Lansing, John"),
        subfieldChangeEmpty("b"),
        subfieldChangeEmpty("c"),
        subfieldChange("d", "1756-1791."),
        subfieldChangeEmpty("j"),
        subfieldChange("q", "(Jules)")
      )),
      new FieldChange().field("600").subfields(List.of(
        subfieldChange("a", "Lansing, John"),
        subfieldChangeEmpty("b"),
        subfieldChangeEmpty("c"),
        subfieldChange("d", "1756-1791."),
        subfieldChangeEmpty("f"),
        subfieldChangeEmpty("g"),
        subfieldChangeEmpty("h"),
        subfieldChangeEmpty("j"),
        subfieldChangeEmpty("k"),
        subfieldChange("l", "book"),
        subfieldChangeEmpty("m"),
        subfieldChangeEmpty("n"),
        subfieldChangeEmpty("o"),
        subfieldChangeEmpty("p"),
        subfieldChange("q", "(Jules)"),
        subfieldChangeEmpty("r"),
        subfieldChangeEmpty("s"),
        subfieldChange("t", "Black Eagles")
      )),
      new FieldChange().field("700").subfields(List.of(
        subfieldChange("a", "Lansing, John"),
        subfieldChangeEmpty("b"),
        subfieldChangeEmpty("c"),
        subfieldChange("d", "1756-1791."),
        subfieldChangeEmpty("f"),
        subfieldChangeEmpty("g"),
        subfieldChangeEmpty("h"),
        subfieldChangeEmpty("j"),
        subfieldChangeEmpty("k"),
        subfieldChange("l", "book"),
        subfieldChangeEmpty("m"),
        subfieldChangeEmpty("n"),
        subfieldChangeEmpty("o"),
        subfieldChangeEmpty("p"),
        subfieldChange("q", "(Jules)"),
        subfieldChangeEmpty("r"),
        subfieldChangeEmpty("s"),
        subfieldChange("t", "Black Eagles")
      )),
      new FieldChange().field("800").subfields(List.of(
        subfieldChange("a", "Lansing, John"),
        subfieldChangeEmpty("b"),
        subfieldChangeEmpty("c"),
        subfieldChange("d", "1756-1791."),
        subfieldChangeEmpty("f"),
        subfieldChangeEmpty("g"),
        subfieldChangeEmpty("h"),
        subfieldChangeEmpty("j"),
        subfieldChangeEmpty("k"),
        subfieldChange("l", "book"),
        subfieldChangeEmpty("m"),
        subfieldChangeEmpty("n"),
        subfieldChangeEmpty("o"),
        subfieldChangeEmpty("p"),
        subfieldChange("q", "(Jules)"),
        subfieldChangeEmpty("r"),
        subfieldChangeEmpty("s"),
        subfieldChange("t", "Black Eagles")
      )),
      new FieldChange().field("240").subfields(List.of(
        subfieldChange("a", "Black Eagles"),
        subfieldChangeEmpty("f"),
        subfieldChangeEmpty("g"),
        subfieldChangeEmpty("h"),
        subfieldChangeEmpty("k"),
        subfieldChange("l", "book"),
        subfieldChangeEmpty("m"),
        subfieldChangeEmpty("n"),
        subfieldChangeEmpty("o"),
        subfieldChangeEmpty("p"),
        subfieldChangeEmpty("r"),
        subfieldChangeEmpty("s")
      ))
    );
    assertions.then(value.getJobId()).as("Job ID").isNotNull();
    assertions.then(value.getTs()).as("Timestamp").isNotNull();

    assertions.assertAll();
  }

  @NotNull
  private List<ChangeTarget> getChangeTargets(LinksChangeEvent value) {
    for (ChangeTarget changeTarget : value.getUpdateTargets()) {
      for (ChangeTargetLink l : changeTarget.getLinks()) {
        l.setLinkId(null);
      }
    }
    return value.getUpdateTargets();
  }

  private SubfieldChange subfieldChange(String code, String value) {
    return new SubfieldChange().code(code).value(value);
  }

  private SubfieldChange subfieldChangeEmpty(String code) {
    return subfieldChange(code, EMPTY);
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
