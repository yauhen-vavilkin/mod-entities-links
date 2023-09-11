package org.folio.entlinks.controller;

import static java.util.UUID.randomUUID;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_ID;
import static org.folio.entlinks.config.constants.ErrorCode.VIOLATION_OF_RELATION_BETWEEN_AUTHORITY_AND_SOURCE_FILE;
import static org.folio.entlinks.service.reindex.event.DomainEventType.CREATE;
import static org.folio.entlinks.service.reindex.event.DomainEventType.DELETE;
import static org.folio.entlinks.service.reindex.event.DomainEventType.UPDATE;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.TestDataUtils.AuthorityTestData.authorityDto;
import static org.folio.support.TestDataUtils.AuthorityTestData.authoritySourceFile;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.authoritySourceFilesEndpoint;
import static org.folio.support.base.TestConstants.authorityTopic;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityDtoCollection;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.exception.AuthorityNotFoundException;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.service.reindex.event.DomainEvent;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.base.IntegrationTestBase;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE,
  DatabaseHelper.AUTHORITY_DATA_STAT_TABLE,
  DatabaseHelper.AUTHORITY_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE})
class AuthorityControllerIT extends IntegrationTestBase {

  private static final List<String> DOMAIN_EVENT_HEADER_KEYS =
      List.of(XOkapiHeaders.TENANT, XOkapiHeaders.URL, XOkapiHeaders.USER_ID, DOMAIN_EVENT_HEADER_KEY);

  private KafkaMessageListenerContainer<String, DomainEvent> container;
  private BlockingQueue<ConsumerRecord<String, DomainEvent>> consumerRecords;

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @BeforeEach
  void setUp(@Autowired KafkaProperties kafkaProperties) {
    consumerRecords = new LinkedBlockingQueue<>();
    container = createAndStartTestConsumer(authorityTopic(), consumerRecords, kafkaProperties, DomainEvent.class);
  }

  @AfterEach
  void tearDown() {
    consumerRecords.clear();
    container.stop();
  }

  // Tests for Get Collection

  @Test
  @DisplayName("Get Collection: find no Authority")
  void getCollection_positive_noEntitiesFound() throws Exception {
    doGet(authorityEndpoint())
      .andExpect(jsonPath("totalRecords", is(0)));
  }

  @Test
  @DisplayName("Get Collection: find all Authority entities")
  void getCollection_positive_entitiesFound() throws Exception {
    var createdEntities = createAuthorities();

    tryGet(authorityEndpoint())
      .andExpect(status().isOk())
      .andExpect(jsonPath("totalRecords", is(createdEntities.size())))
      .andExpect(jsonPath("authorities[0].metadata", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("authorities[0].metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.updatedByUserId", is(USER_ID)));
  }

  @ParameterizedTest
  @CsvSource({
    "0, 3, descending, source3",
    "1, 3, ascending, source2",
    "2, 2, descending, source1"
  })
  @DisplayName("Get Collection: return list of authorities for the given limit and offset")
  void getCollection_positive_entitiesSortedByNameAndLimitedWithOffset(String offset, String limit, String sortOrder,
                                                                       String firstSourceName) throws Exception {
    createAuthorities();
    // the following two authorities should be filtered out and not included in the result because of deleted = true
    createSourceFile(1);
    var authority1 = authority(0, 1);
    authority1.setId(UUID.randomUUID());
    authority1.setDeleted(true);
    var authority2 = authority(0, 1);
    authority2.setId(UUID.randomUUID());
    authority2.setDeleted(true);
    databaseHelper.saveAuthority(TENANT_ID, authority1);
    databaseHelper.saveAuthority(TENANT_ID, authority2);

    var cqlQuery = "(cql.allRecords=1)sortby source/sort." + sortOrder;
    doGet(authorityEndpoint() + "?limit={l}&offset={o}&query={cql}", limit, offset, cqlQuery)
      .andExpect(jsonPath("authorities[0].source", is(firstSourceName)))
      .andExpect(jsonPath("authorities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("totalRecords").value(3));
  }

  // Tests for Get By ID

  @Test
  @DisplayName("Get By ID: return authority by given ID")
  void getById_positive_foundByIdForExistingEntity() throws Exception {
    createSourceFile(0);
    var authority = createAuthority(0, 0);

    doGet(authorityEndpoint(authority.getId()))
      .andExpect(jsonPath("source", is(authority.getSource())))
      .andExpect(jsonPath("naturalId", is(authority.getNaturalId())))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)));
  }

  // Tests for POST

  @Test
  @DisplayName("POST: create new Authority with defined ID")
  void createAuthority_positive_entityCreatedWithProvidedId() throws Exception {
    assumeTrue(databaseHelper.countRows(DatabaseHelper.AUTHORITY_TABLE, TENANT_ID) == 0);

    var dto = authorityDto(0, 0);
    var id = randomUUID();
    dto.setId(id);
    createSourceFile(0);

    var content = tryPost(authorityEndpoint(), dto)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("id", is(id.toString())))
      .andExpect(jsonPath("source", is(dto.getSource())))
      .andExpect(jsonPath("naturalId", is(dto.getNaturalId())))
      .andExpect(jsonPath("personalName", is(dto.getPersonalName())))
      .andExpect(jsonPath("sourceFileId", is(dto.getSourceFileId().toString())))
      .andExpect(jsonPath("_version", is(0)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)))
      .andReturn().getResponse().getContentAsString();

    var created = objectMapper.readValue(content, AuthorityDto.class);
    var receivedEvent = getReceivedEvent();

    verifyReceivedDomainEvent(receivedEvent, CREATE, DOMAIN_EVENT_HEADER_KEYS, created, AuthorityDto.class,
        "metadata.createdDate", "metadata.updatedDate");
    assertEquals(1, databaseHelper.countRows(DatabaseHelper.AUTHORITY_TABLE, TENANT_ID));
    assertEquals(dto.getNotes(), created.getNotes());
    assertEquals(dto.getIdentifiers(), created.getIdentifiers());
    assertEquals(dto.getSftPersonalName(), created.getSftPersonalName());
    assertEquals(dto.getSaftPersonalName(), created.getSaftPersonalName());
  }

  @Test
  @DisplayName("POST: create new Authority without defined ID")
  void createAuthority_positive_entityCreatedWithNewId() throws Exception {
    assumeTrue(databaseHelper.countRows(DatabaseHelper.AUTHORITY_TABLE, TENANT_ID) == 0);

    var dto = authorityDto(0, 0);
    createSourceFile(0);

    var content = tryPost(authorityEndpoint(), dto)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("id", notNullValue()))
      .andExpect(jsonPath("source", is(dto.getSource())))
      .andExpect(jsonPath("naturalId", is(dto.getNaturalId())))
      .andExpect(jsonPath("personalName", is(dto.getPersonalName())))
      .andExpect(jsonPath("sourceFileId", is(dto.getSourceFileId().toString())))
      .andExpect(jsonPath("_version", is(0)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)))
      .andReturn().getResponse().getContentAsString();

    var created = objectMapper.readValue(content, AuthorityDto.class);
    getReceivedEvent();

    assertEquals(1, databaseHelper.countRows(DatabaseHelper.AUTHORITY_TABLE, TENANT_ID));
    assertEquals(dto.getNotes(), created.getNotes());
    assertEquals(dto.getIdentifiers(), created.getIdentifiers());
    assertEquals(dto.getSftPersonalName(), created.getSftPersonalName());
    assertEquals(dto.getSaftPersonalName(), created.getSaftPersonalName());
  }

  @Test
  @DisplayName("POST: create new Authority without Source File ID")
  void createAuthority_positive_entityWithoutSourceFileRelationShouldBeCreated() throws Exception {
    assumeTrue(databaseHelper.countRows(DatabaseHelper.AUTHORITY_TABLE, TENANT_ID) == 0);

    var dto = authorityDto(0, 0);
    dto.setSourceFileId(null);

    tryPost(authorityEndpoint(), dto)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("source", is(dto.getSource())))
      .andExpect(jsonPath("naturalId", is(dto.getNaturalId())))
      .andExpect(jsonPath("personalName", is(dto.getPersonalName())))
      .andExpect(jsonPath("sourceFileId").doesNotExist())
      .andExpect(jsonPath("_version", is(0)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)));
    getReceivedEvent();

    assertEquals(1, databaseHelper.countRows(DatabaseHelper.AUTHORITY_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("POST: create new Authority with non-existing source file")
  void createAuthority_negative_notCreatedWithNonExistingSourceFile() throws Exception {
    assumeTrue(databaseHelper.countRows(DatabaseHelper.AUTHORITY_TABLE, TENANT_ID) == 0);

    var dto = authorityDto(0, 0);
    var sourceFileId = randomUUID();
    dto.setSourceFileId(sourceFileId);

    tryPost(authorityEndpoint(), dto)
      .andExpect(status().isNotFound())
      .andExpect(errorMessageMatch(is("Authority Source File with ID [" + sourceFileId + "] was not found")))
      .andExpect(exceptionMatch(AuthoritySourceFileNotFoundException.class));
  }

  @Test
  @DisplayName("POST: create new Authority with duplicate id")
  void createAuthority_negative_notCreatedWithDuplicatedId() throws Exception {
    assumeTrue(databaseHelper.countRows(DatabaseHelper.AUTHORITY_TABLE, TENANT_ID) == 0);

    var dto = authorityDto(0, 0);
    dto.setId(randomUUID());
    dto.setSourceFileId(null);

    tryPost(authorityEndpoint(), dto).andExpect(status().isCreated());
    getReceivedEvent();

    tryPost(authorityEndpoint(), dto)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is(DUPLICATE_AUTHORITY_ID.getMessage())))
      .andExpect(exceptionMatch(DataIntegrityViolationException.class));
  }

  // Tests for PUT

  @Test
  @DisplayName("PUT: update existing Authority entity")
  void updateAuthority_positive_entityUpdated() throws Exception {
    getReceivedEvent();
    var dto = authorityDto(0, 0);
    createSourceFile(0);

    doPost(authorityEndpoint(), dto);
    getReceivedEvent();
    var existingAsString = doGet(authorityEndpoint()).andReturn().getResponse().getContentAsString();
    var collection = objectMapper.readValue(existingAsString, AuthorityDtoCollection.class);
    var expected = collection.getAuthorities().get(0);
    expected.setSource("updated source");
    expected.setPersonalName(null);
    expected.setCorporateName("headingCorporateName");
    expected.setSftCorporateName(List.of("sftCorporateName"));
    expected.setSaftCorporateName(List.of("saftCorporateName"));

    tryPut(authorityEndpoint(expected.getId()), expected).andExpect(status().isNoContent());

    var content = doGet(authorityEndpoint(expected.getId()))
      .andExpect(jsonPath("source", is(expected.getSource())))
      .andExpect(jsonPath("naturalId", is(expected.getNaturalId())))
      .andExpect(jsonPath("sourceFileId", is(expected.getSourceFileId().toString())))
      .andExpect(jsonPath("personalName").doesNotExist())
      .andExpect(jsonPath("corporateName", is(expected.getCorporateName())))
      .andExpect(jsonPath("_version", is(1)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)))
      .andReturn().getResponse().getContentAsString();

    var resultDto = objectMapper.readValue(content, AuthorityDto.class);
    var receivedEvent = getReceivedEvent();
    awaitUntilAsserted(() ->
        assertEquals(1, databaseHelper.countRows(DatabaseHelper.AUTHORITY_DATA_STAT_TABLE, TENANT_ID)));

    verifyReceivedDomainEvent(receivedEvent, UPDATE, DOMAIN_EVENT_HEADER_KEYS, resultDto, AuthorityDto.class,
        "metadata.createdDate", "metadata.updatedDate");
    assertEquals(expected.getNotes(), resultDto.getNotes());
    assertEquals(expected.getIdentifiers(), resultDto.getIdentifiers());
    assertEquals(expected.getSftPersonalName(), resultDto.getSftPersonalName());
    assertEquals(expected.getSaftPersonalName(), resultDto.getSaftPersonalName());
    assertEquals(expected.getSftCorporateName(), resultDto.getSftCorporateName());
    assertEquals(expected.getSaftCorporateName(), resultDto.getSaftCorporateName());
  }

  @Test
  @Disabled("behaves un-deterministically and thus disable for now")
  @DisplayName("PUT: repeated update of Authority without modification")
  void updateConcurrently_positive_notAllShouldSucceedAndAtLeastOneShouldFail() throws Exception {
    var dto = authorityDto(0, 0);
    createSourceFile(0);

    doPost(authorityEndpoint(), dto);
    getReceivedEvent();
    var existingAsString = doGet(authorityEndpoint()).andReturn().getResponse().getContentAsString();
    var collection = objectMapper.readValue(existingAsString, AuthorityDtoCollection.class);
    var expected = collection.getAuthorities().get(0);
    var sources = List.of("source a", "source b", "source c", "source d");

    int concurrency = 4;
    ExecutorService executor = Executors.newFixedThreadPool(concurrency);
    for (var source : sources) {
      executor.execute(() -> {
        expected.setSource(source);
        doPut(authorityEndpoint(expected.getId()), expected);
      });
    }
    executor.shutdown();
    assertTrue(executor.awaitTermination(2, TimeUnit.MINUTES));

    doGet(authorityEndpoint(expected.getId()))
      .andExpect(jsonPath("_version", lessThan(concurrency)))
      .andExpect(jsonPath("source",
        anyOf(equalTo("source a"), equalTo("source b"),
          equalTo("source c"), equalTo("source d"))))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)));
  }

  @Test
  @DisplayName("PUT: update Authority with non-existing source file id")
  void updateAuthority_negative_notUpdatedWithNonExistingSourceFile() throws Exception {
    var dto = authorityDto(0, 0);
    createSourceFile(0);

    doPost(authorityEndpoint(), dto);
    getReceivedEvent();
    var existingAsString = doGet(authorityEndpoint()).andReturn().getResponse().getContentAsString();
    var collection = objectMapper.readValue(existingAsString, AuthorityDtoCollection.class);
    var expected = collection.getAuthorities().get(0);
    var sourceFileId = randomUUID();
    expected.setSourceFileId(sourceFileId);

    tryPut(authorityEndpoint(expected.getId()), expected)
      .andExpect(status().isNotFound())
      .andExpect(errorMessageMatch(is("Authority Source File with ID [" + sourceFileId + "] was not found")))
      .andExpect(exceptionMatch(AuthoritySourceFileNotFoundException.class));
  }

  @Test
  @DisplayName("PUT: return 404 for non-existing authority")
  void updateAuthority_negative_entityNotFound() throws Exception {
    var id = UUID.randomUUID();
    var dto = new AuthorityDto().id(id);

    tryPut(authorityEndpoint(id), dto).andExpect(status().isNotFound())
      .andExpect(exceptionMatch(AuthorityNotFoundException.class))
      .andExpect(errorMessageMatch(containsString("was not found")));
  }

  // Tests for DELETE

  @Test
  @DisplayName("DELETE: Should delete existing authority")
  void deleteAuthority_positive_deleteExistingEntity() throws Exception {
    createSourceFile(0);
    var authority = createAuthority(0, 0);

    var contentAsString = doGet(authorityEndpoint(authority.getId())).andReturn().getResponse().getContentAsString();
    var expectedDto = objectMapper.readValue(contentAsString, AuthorityDto.class);

    doDelete(authorityEndpoint(authority.getId()));
    var receivedEvent = getReceivedEvent();
    awaitUntilAsserted(() ->
        assertEquals(1, databaseHelper.countRows(DatabaseHelper.AUTHORITY_DATA_STAT_TABLE, TENANT_ID)));

    verifyReceivedDomainEvent(receivedEvent, DELETE, DOMAIN_EVENT_HEADER_KEYS, expectedDto, AuthorityDto.class,
        "metadata.createdDate", "metadata.updatedDate");
    tryGet(authorityEndpoint(authority.getId()))
        .andExpect(status().isNotFound())
        .andExpect(exceptionMatch(AuthorityNotFoundException.class));
  }

  @Test
  @DisplayName("DELETE: Return 404 for non-existing entity")
  void deleteAuthority_negative_entityNotFound() throws Exception {

    tryDelete(authorityEndpoint(UUID.randomUUID())).andExpect(status().isNotFound())
      .andExpect(exceptionMatch(AuthorityNotFoundException.class))
      .andExpect(errorMessageMatch(containsString("was not found")));
  }

  @Test
  @DisplayName("DELETE: Return 400 for invalid request id")
  void deleteAuthority_negative_invalidProvidedRequestId() throws Exception {

    tryDelete(authorityEndpoint() + "/{id}", "invalid-uuid").andExpect(status().isBadRequest())
      .andExpect(exceptionMatch(MethodArgumentTypeMismatchException.class))
      .andExpect(errorMessageMatch(containsString(
        "Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'")));
  }

  @Test
  @DisplayName("DELETE: Authority Source File cannot be deleted when it is linked by Authority")
  void deleteAuthoritySourceFile_negative_failDeletingSourceFileLinkedByAuthority() throws Exception {
    var dto = authorityDto(0, 0);
    createSourceFile(0);

    doPost(authorityEndpoint(), dto);
    getReceivedEvent();
    var existingAsString = doGet(authorityEndpoint())
      .andReturn().getResponse().getContentAsString();
    var collection = objectMapper.readValue(existingAsString, AuthorityDtoCollection.class);
    var expected = collection.getAuthorities().get(0);

    assertEquals(dto.getSourceFileId(), expected.getSourceFileId());

    tryDelete(authoritySourceFilesEndpoint(expected.getSourceFileId()))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is(VIOLATION_OF_RELATION_BETWEEN_AUTHORITY_AND_SOURCE_FILE.getMessage())))
      .andExpect(exceptionMatch(DataIntegrityViolationException.class));
  }

  private List<Authority> createAuthorities() {
    createSourceFile(0);
    var entity1 = createAuthority(0, 0);
    var entity2 = createAuthority(1, 0);
    var entity3 = createAuthority(2, 0);

    return List.of(entity1, entity2, entity3);
  }

  private Authority createAuthority(int authorityNum, int sourceFileNum) {
    var entity = authority(authorityNum, sourceFileNum);
    databaseHelper.saveAuthority(TENANT_ID, entity);
    return entity;
  }

  private void createSourceFile(int sourceFileNum) {
    var entity = authoritySourceFile(sourceFileNum);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, entity);

    entity.getAuthoritySourceFileCodes().forEach(code ->
        databaseHelper.saveAuthoritySourceFileCode(TENANT_ID, entity.getId(), code));

  }

  private ResultMatcher errorMessageMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].message", errorMessageMatcher);
  }

  @Nullable
  @SneakyThrows
  private ConsumerRecord<String, DomainEvent> getReceivedEvent() {
    return consumerRecords.poll(10, TimeUnit.SECONDS);
  }

}
