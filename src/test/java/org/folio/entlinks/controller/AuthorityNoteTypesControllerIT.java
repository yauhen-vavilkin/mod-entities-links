package org.folio.entlinks.controller;

import static java.util.UUID.randomUUID;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.folio.support.base.TestConstants.authorityNoteTypesEndpoint;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.entlinks.config.JpaConfig;
import org.folio.entlinks.domain.dto.AuthorityNoteTypeDto;
import org.folio.entlinks.domain.dto.AuthorityNoteTypeDtoCollection;
import org.folio.entlinks.domain.entity.AuthorityNoteType;
import org.folio.entlinks.exception.AuthorityNoteTypeNotFoundException;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.base.IntegrationTestBase;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


@IntegrationTest
@DatabaseCleanup(tables = {DatabaseHelper.AUTHORITY_NOTE_TYPE_TABLE})
@Import(JpaConfig.class)
class AuthorityNoteTypesControllerIT extends IntegrationTestBase {

  private static final String CREATED_DATE = "2021-10-28T06:31:31+05:00";

  private static final UUID[] NOTE_TYPE_IDS = new UUID[] {randomUUID(), randomUUID(), randomUUID()};
  private static final String[] NOTE_TYPE_NAMES = new String[] {"name1", "name2", "name3"};
  private static final String[] NOTE_TYPE_SOURCES = new String[] {"source1", "source2", "source3"};

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  // Tests for Get Collection
  @Test
  @DisplayName("Get Collection: find no Authority Note Types")
  void getAuthorityNoteTypes_positive_noEntitiesFound() throws Exception {
    doGet(authorityNoteTypesEndpoint())
        .andExpect(jsonPath("totalRecords", is(0)));
  }

  @Test
  @DisplayName("Get Collection: find all Authority Note Types")
  void getCollection_positive_entitiesFound() throws Exception {
    var createdEntities = createAuthorityNoteTypes();

    doGet(authorityNoteTypesEndpoint())
        .andExpect(jsonPath("totalRecords", is(createdEntities.size())))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata", notNullValue()))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.createdDate", notNullValue()))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.updatedDate", notNullValue()))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.createdByUserId", is(USER_ID)))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.updatedByUserId", is(USER_ID)));
  }

  @MethodSource("cqlQueryProvider")
  @ParameterizedTest
  @DisplayName("Get Collection: find only one note type by CQL")
  void getCollection_positive_entitiesFoundByCqlQuery(String cqlQuery) throws Exception {
    createAuthorityNoteTypes();

    doGet(authorityNoteTypesEndpoint() + "?query=({cql})", cqlQuery)
        .andExpect(status().isOk())
        .andExpect(jsonPath("authorityNoteTypes[0].id", is(NOTE_TYPE_IDS[0].toString())))
        .andExpect(jsonPath("authorityNoteTypes[0].source", is(NOTE_TYPE_SOURCES[0])))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.createdDate", notNullValue()))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.updatedDate", notNullValue()))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.createdByUserId", is(USER_ID)))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.updatedByUserId", is(USER_ID)))
        .andExpect(jsonPath("authorityNoteTypes[1]").doesNotExist())
        .andExpect(jsonPath("totalRecords").value(1));
  }

  @Test
  @DisplayName("Get Collection: find all note types sorted by name and limited with offset")
  void getCollection_positive_entitiesSortedByNameAndLimitedWithOffset() throws Exception {
    var noteTypes = createAuthorityNoteTypes();

    var cqlQuery = "(cql.allRecords=1)sortby name/sort.descending";
    var limit = "1";
    var offset = "1";
    doGet(authorityNoteTypesEndpoint() + "?limit={l}&offset={o}&query={cql}", limit, offset, cqlQuery)
        .andExpect(jsonPath("authorityNoteTypes[0].id", is(NOTE_TYPE_IDS[1].toString())))
        .andExpect(jsonPath("authorityNoteTypes[0].name", is(NOTE_TYPE_NAMES[1])))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.createdDate", notNullValue()))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.updatedDate", notNullValue()))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.createdByUserId", is(USER_ID)))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.updatedByUserId", is(USER_ID)))
        .andExpect(jsonPath("authorityNoteTypes[1]").doesNotExist())
        .andExpect(jsonPath("totalRecords").value(noteTypes.size()));
  }

  @ParameterizedTest
  @CsvSource({
    "0, 3, descending, name3",
    "1, 3, ascending, name2",
    "2, 2, descending, name1"
  })
  @DisplayName("Get Collection: return list of note types for the given limit and offset")
  void getCollection_positive_entitiesSortedByNameAndLimitedWithOffset(String offset, String limit, String sortOrder,
                                                                       String firstNoteTypeName) throws Exception {
    createAuthorityNoteTypes();

    var cqlQuery = "(cql.allRecords=1)sortby name/sort." + sortOrder;
    doGet(authorityNoteTypesEndpoint() + "?limit={l}&offset={o}&query={cql}", limit, offset, cqlQuery)
        .andExpect(jsonPath("authorityNoteTypes[0].name", is(firstNoteTypeName)))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.createdDate", notNullValue()))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.updatedDate", notNullValue()))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.createdByUserId", is(USER_ID)))
        .andExpect(jsonPath("authorityNoteTypes[0].metadata.updatedByUserId", is(USER_ID)))
        .andExpect(jsonPath("totalRecords").value(3));
  }

  // Tests for Get By ID

  @Test
  @DisplayName("Get By ID: return note type by given ID")
  void getById_positive_foundByIdForExistingEntity() throws Exception {
    var noteType = prepareAuthorityNoteType(0);
    createAuthorityNoteType(noteType);

    doGet(authorityNoteTypesEndpoint(noteType.getId()))
        .andExpect(jsonPath("name", is(noteType.getName())))
        .andExpect(jsonPath("metadata.createdDate", notNullValue()))
        .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
        .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)))
        .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)));
  }

  @Test
  @DisplayName("Get By ID: return 404 for not existing entity")
  void getById_negative_noAuthorityNoteTypeExistForGivenId() throws Exception {

    tryGet(authorityNoteTypesEndpoint(UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(exceptionMatch(AuthorityNoteTypeNotFoundException.class))
        .andExpect(errorMessageMatch(containsString("was not found")));
  }

  @Test
  @DisplayName("Get By ID: return 400 when id is invalid")
  void getById_negative_IdIsInvalid() throws Exception {
    tryGet(authorityNoteTypesEndpoint() + "/{id}", "invalid-uuid")
        .andExpect(status().isBadRequest())
        .andExpect(exceptionMatch(MethodArgumentTypeMismatchException.class))
        .andExpect(errorMessageMatch(containsString(
            "Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'")));
  }

  // Tests for POST

  @Test
  @DisplayName("POST: create new Authority Note Type")
  void createAuthorityNoteType_positive_entityCreated() throws Exception {
    assumeTrue(databaseHelper.countRows(DatabaseHelper.AUTHORITY_NOTE_TYPE_TABLE, TENANT_ID) == 0);

    var dto = new AuthorityNoteTypeDto("name", "source");

    tryPost(authorityNoteTypesEndpoint(), dto)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("name", is(dto.getName())))
        .andExpect(jsonPath("source", is(dto.getSource())))
        .andExpect(jsonPath("metadata.createdDate").isNotEmpty())
        .andExpect(jsonPath("metadata.updatedDate").isNotEmpty())
        .andExpect(jsonPath("metadata.updatedByUserId").isNotEmpty())
        .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)));

    assertEquals(1, databaseHelper.countRows(DatabaseHelper.AUTHORITY_NOTE_TYPE_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("POST: return 422 for authority note type without name")
  void createAuthorityNoteType_negative_entityWithoutNameNotCreated() throws Exception {
    var dto = new AuthorityNoteTypeDto(null, "source");

    tryPost(authorityNoteTypesEndpoint(), dto)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(exceptionMatch(MethodArgumentNotValidException.class))
        .andExpect(jsonPath("$.errors.[0].parameters[0].key", is("name")))
        .andExpect(jsonPath("$.errors.[0].parameters[0].value", is("null")))
        .andExpect(errorMessageMatch(containsString("must not be null")));

  }

  // Tests for PUT

  @Test
  @DisplayName("PUT: update existing note type")
  void updateAuthorityNoteType_positive_entityUpdated() throws Exception {
    var dto = new AuthorityNoteTypeDto("name", "source");

    doPost(authorityNoteTypesEndpoint(), dto);
    var existingAsString = doGet(authorityNoteTypesEndpoint()).andReturn().getResponse().getContentAsString();
    var collection = objectMapper.readValue(existingAsString, AuthorityNoteTypeDtoCollection.class);
    var modified = collection.getAuthorityNoteTypes().get(0);
    modified.setName("updated name");
    modified.setSource("updated source");

    tryPut(authorityNoteTypesEndpoint(modified.getId()), modified).andExpect(status().isNoContent());

    doGet(authorityNoteTypesEndpoint(modified.getId()))
        .andExpect(jsonPath("name", is(modified.getName())))
        .andExpect(jsonPath("source", is(modified.getSource())))
        .andExpect(jsonPath("metadata.updatedDate").isNotEmpty())
        .andExpect(jsonPath("metadata.updatedByUserId").isNotEmpty());
  }

  @Test
  @DisplayName("PUT: return 404 for non-existing note type")
  void updateAuthorityNoteType_negative_entityNotFound() throws Exception {
    var id = UUID.randomUUID();
    var dto = new AuthorityNoteTypeDto("name", "source").id(id);

    tryPut(authorityNoteTypesEndpoint(id), dto).andExpect(status().isNotFound())
        .andExpect(exceptionMatch(AuthorityNoteTypeNotFoundException.class))
        .andExpect(errorMessageMatch(containsString("was not found")));
  }

  @Test
  @DisplayName("PUT: return 422 for invalid note type dto")
  void updateAuthorityNoteType_negative_invalidRequestDto() throws Exception {
    var id = UUID.randomUUID();
    var dto = new AuthorityNoteTypeDto(null, "source").id(id);

    tryPut(authorityNoteTypesEndpoint(id), dto).andExpect(status().isUnprocessableEntity())
        .andExpect(exceptionMatch(MethodArgumentNotValidException.class))
        .andExpect(jsonPath("$.errors.[0].parameters[0].key", is("name")))
        .andExpect(jsonPath("$.errors.[0].parameters[0].value", is("null")))
        .andExpect(errorMessageMatch(containsString("must not be null")));
  }

  // Tests for DELETE

  @Test
  @DisplayName("DELETE: Should delete existing authority note type")
  void deleteAuthorityNoteType_positive_deleteExistingEntity() {
    var noteType = prepareAuthorityNoteType(0);
    createAuthorityNoteType(noteType);

    doDelete(authorityNoteTypesEndpoint(noteType.getId()));

    assertNull(databaseHelper.getAuthorityNoteTypeById(noteType.getId(), TENANT_ID));
  }

  @Test
  @DisplayName("DELETE: Return 404 for non-existing entity")
  void deleteAuthorityNoteType_negative_entityNotFound() throws Exception {

    tryDelete(authorityNoteTypesEndpoint(UUID.randomUUID())).andExpect(status().isNotFound())
        .andExpect(exceptionMatch(AuthorityNoteTypeNotFoundException.class))
        .andExpect(errorMessageMatch(containsString("was not found")));
  }

  @Test
  @DisplayName("DELETE: Return 400 for invalid authority note type id")
  void deleteAuthorityNoteType_negative_invalidProvidedRequestId() throws Exception {

    tryDelete(authorityNoteTypesEndpoint() + "/{id}", "invalid-uuid").andExpect(status().isBadRequest())
        .andExpect(exceptionMatch(MethodArgumentTypeMismatchException.class))
        .andExpect(errorMessageMatch(containsString(
            "Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'")));
  }

  private static Stream<Arguments> cqlQueryProvider() {
    return Stream.of(
        arguments("id = " + NOTE_TYPE_IDS[0]),
        arguments("name <= " + NOTE_TYPE_NAMES[0]),
        arguments("source <= " + NOTE_TYPE_SOURCES[0])
    );
  }

  private List<AuthorityNoteType> createAuthorityNoteTypes() {
    var noteType1 = prepareAuthorityNoteType(0);
    var noteType2 = prepareAuthorityNoteType(1);
    var noteType3 = prepareAuthorityNoteType(2);

    createAuthorityNoteType(noteType1);
    createAuthorityNoteType(noteType2);
    createAuthorityNoteType(noteType3);

    return List.of(noteType1, noteType2, noteType3);
  }

  private AuthorityNoteType prepareAuthorityNoteType(int i) {
    var entity = new AuthorityNoteType();
    entity.setId(NOTE_TYPE_IDS[i]);
    entity.setName(NOTE_TYPE_NAMES[i]);
    entity.setSource(NOTE_TYPE_SOURCES[i]);
    entity.setCreatedDate(Timestamp.from(Instant.parse(CREATED_DATE)));
    entity.setCreatedByUserId(UUID.fromString(USER_ID));
    entity.setUpdatedDate(Timestamp.from(Instant.parse(CREATED_DATE)));
    entity.setUpdatedByUserId(UUID.fromString(USER_ID));
    return entity;
  }

  private void createAuthorityNoteType(AuthorityNoteType noteType) {
    databaseHelper.saveAuthorityNoteType(TENANT_ID, noteType);
  }

  private ResultMatcher errorMessageMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].message", errorMessageMatcher);
  }
}
