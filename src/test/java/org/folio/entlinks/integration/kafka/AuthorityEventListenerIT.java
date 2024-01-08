package org.folio.entlinks.integration.kafka;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.AUTHORITY_IDS;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.TestDataUtils.NATURAL_IDS;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.UPDATE_TYPE;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.authorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceAuthorityTopic;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.ChangeTarget;
import org.folio.entlinks.domain.dto.ChangeTargetLink;
import org.folio.entlinks.domain.dto.FieldChange;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.domain.dto.SubfieldChange;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.TestDataUtils.Link;
import org.folio.support.base.IntegrationTestBase;
import org.jetbrains.annotations.NotNull;
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
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE
})
class AuthorityEventListenerIT extends IntegrationTestBase {

  private static final UUID AUTHORITY_ID = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f325e");
  private static final UUID SOURCE_FILE_ID = UUID.fromString("af045f2f-e851-4613-984c-4bc13430454a");
  private static final String BASE_URL = "id.loc.gov/authorities/names/";

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

    var sourceFile1 = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    var sourceFile2 = new AuthoritySourceFile(sourceFile1);
    sourceFile2.setId(SOURCE_FILE_ID);
    sourceFile2.setName("LC Name Authority file (LCNAF)");
    sourceFile2.setBaseUrl(BASE_URL);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, sourceFile1);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, sourceFile2);

    var authority1 = authority(0, 0);
    databaseHelper.saveAuthority(TENANT_ID, authority1);
    var authority2 = authority(0, 0);
    authority2.setId(AUTHORITY_ID);
    databaseHelper.saveAuthority(TENANT_ID, authority2);
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
    var link1 = Link.of(0, 0, NATURAL_IDS[0]);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId1, link1)), instanceId1);
    var link2 = Link.of(0, 2, NATURAL_IDS[0]);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId2, link2)), instanceId2);
    var link3 = Link.of(0, 0, NATURAL_IDS[0]);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId3, link3)), instanceId3);

    var event = TestDataUtils.authoritySoftDeleteEvent(null,
      new AuthorityDto().id(link1.authorityId()).naturalId("oldNaturalId"));
    sendKafkaMessage(authorityTopic(), link1.authorityId().toString(), event);

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
    var authorityId = AUTHORITY_IDS[0];
    var instanceId = UUID.randomUUID();
    var naturalId = NATURAL_IDS[0];
    var link = new Link(authorityId, "240", naturalId, new char[] {'a', 'b', 'c'});
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId, link)), instanceId);

    // prepare and send inventory update authority event
    var event = TestDataUtils.authorityEvent(UPDATE_TYPE,
      new AuthorityDto().id(authorityId).personalName("new personal name").naturalId(naturalId),
      new AuthorityDto().id(authorityId).personalNameTitle("old").naturalId(naturalId));
    sendKafkaMessage(authorityTopic(), authorityId.toString(), event);

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
    assertions.then(value.getAuthorityId()).as("Authority ID").isEqualTo(link.authorityId());
    assertions.then(getChangeTargets(value)).as("Update targets")
      .isEqualTo(List.of(
        updateTarget(link.tag(), instanceId)
      ));
    assertions.then(value.getSubfieldsChanges()).as("Subfield changes").isEmpty();
    assertions.then(value.getJobId()).as("Job ID").isNotNull();
    assertions.then(value.getTs()).as("Timestamp").isNotNull();

    assertions.assertAll();

    // check that links were deleted
    doGet(linksInstanceEndpoint(), instanceId)
      .andExpect(jsonPath("$.links", hasSize(0)));
  }

  @SneakyThrows
  @Test
  void shouldHandleUpdateEvent_positive_whenAuthorityLinkExistAndOnlyNaturalIdChanged() {
    var instanceId1 = UUID.randomUUID();
    var instanceId2 = UUID.randomUUID();
    var instanceId3 = UUID.randomUUID();
    var link1 = Link.of(0, 0, NATURAL_IDS[0]);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId1, link1)), instanceId1);
    var link2 = Link.of(0, 2, NATURAL_IDS[0]);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId2, link2)), instanceId2);
    var link3 = Link.of(0, 0, NATURAL_IDS[0]);
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId3, link3)), instanceId3);

    var existingAuthorityContent = doGet(authorityEndpoint(link1.authorityId()))
      .andReturn().getResponse().getContentAsString();
    var authorityDto = objectMapper.readValue(existingAuthorityContent, AuthorityDto.class);
    var updatedNaturalId = "newNaturalId";
    authorityDto.setSourceFileId(SOURCE_FILE_ID);
    authorityDto.setNaturalId(updatedNaturalId);
    tryPut(authorityEndpoint(link1.authorityId()), authorityDto).andExpect(status().isNoContent());

    var received = getReceivedEvent();

    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();

    var headers = requireNonNull(received).headers();
    assertions.then(headers)
      .as("Headers")
      .extracting(Header::key)
      .contains(XOkapiHeaders.TENANT, XOkapiHeaders.URL, XOkapiHeaders.TOKEN);

    var value = received.value();
    var expectedSubfieldChange = subfieldChange("0", BASE_URL + updatedNaturalId);
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
          .field(link1.tag())
          .subfields(List.of(expectedSubfieldChange)),
        new FieldChange()
          .field(link2.tag())
          .subfields(List.of(expectedSubfieldChange))));
    assertions.then(value.getJobId()).as("Job ID").isNotNull();
    assertions.then(value.getTs()).as("Timestamp").isNotNull();

    assertions.assertAll();

    // check that links were updated according to authority changes
    doGet(linksInstanceEndpoint(), instanceId1)
      .andExpect(jsonPath("$.links[0].authorityNaturalId", equalTo(updatedNaturalId)));
    doGet(linksInstanceEndpoint(), instanceId2)
      .andExpect(jsonPath("$.links[0].authorityNaturalId", equalTo(updatedNaturalId)));
    doGet(linksInstanceEndpoint(), instanceId3)
      .andExpect(jsonPath("$.links[0].authorityNaturalId", equalTo(updatedNaturalId)));
  }

  @SneakyThrows
  @Test
  void shouldHandleUpdateEvent_positive_whenAuthorityLinkExistAndHeadingChanged() {
    // prepare links
    var instanceId = UUID.randomUUID();
    var naturalId = NATURAL_IDS[0];
    var link = new Link(AUTHORITY_ID, "240", naturalId, new char[] {'a', 'b', 'c'});
    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(instanceId, link)), instanceId);

    // prepare and send inventory update authority event
    var event = TestDataUtils.authorityEvent(UPDATE_TYPE,
      new AuthorityDto().id(AUTHORITY_ID).personalName("new personal name").naturalId(naturalId)
        .sourceFileId(SOURCE_FILE_ID),
      new AuthorityDto().id(AUTHORITY_ID).personalName("old").naturalId(naturalId));
    sendKafkaMessage(authorityTopic(), AUTHORITY_ID.toString(), event);

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
    assertions.then(value.getAuthorityId()).as("Authority ID").isEqualTo(link.authorityId());
    assertions.then(getChangeTargets(value)).as("Update targets")
      .isEqualTo(List.of(
        updateTarget(link.tag(), instanceId)
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
    return new ChangeTarget().field(tag).links(Arrays.stream(instanceIds)
      .map(uuid -> new ChangeTargetLink().instanceId(uuid))
      .toList());
  }

}
