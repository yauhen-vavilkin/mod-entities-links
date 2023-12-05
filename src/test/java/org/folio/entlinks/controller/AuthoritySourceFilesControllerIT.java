package org.folio.entlinks.controller;

import static java.util.UUID.randomUUID;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.folio.support.base.TestConstants.authoritySourceFilesEndpoint;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto.SourceEnum;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.base.IntegrationTestBase;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@IntegrationTest
@DatabaseCleanup(tables = {DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE, DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE})
class AuthoritySourceFilesControllerIT extends IntegrationTestBase {

  private static final String CREATED_DATE = "2021-10-28T06:31:31+05:00";

  private static final UUID[] SOURCE_FILE_IDS = new UUID[] {randomUUID(), randomUUID(), randomUUID()};
  private static final Integer[] SOURCE_FILE_CODE_IDS = new Integer[] {1, 2, 3};
  private static final String[] SOURCE_FILE_CODES = new String[] {"code1", "code2", "code3"};
  private static final String[] SOURCE_FILE_NAMES = new String[] {"name1", "name2", "name3"};
  private static final SourceEnum[] SOURCE_FILE_SOURCES =
    new SourceEnum[] {SourceEnum.FOLIO, SourceEnum.LOCAL, SourceEnum.FOLIO};
  private static final String[] SOURCE_FILE_TYPES = new String[] {"type1", "type2", "type3"};
  private static final String[] SOURCE_FILE_URLS = new String[] {"baseUrl1", "baseUrl2", "baseUrl3"};

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  // Tests for Get Collection
  @Test
  @DisplayName("Get Collection: find no Authority Source Files")
  void getAuthoritySourceFiles_positive_noEntitiesFound() throws Exception {
    doGet(authoritySourceFilesEndpoint())
      .andExpect(jsonPath("totalRecords", is(0)));
  }

  @Test
  @DisplayName("Get Collection: find all Authority Source Files")
  void getCollection_positive_entitiesFound() throws Exception {
    var createdEntities = createAuthoritySourceTypes();

    tryGet(authoritySourceFilesEndpoint())
      .andExpect(status().isOk())
      .andExpect(jsonPath("totalRecords", is(createdEntities.size())))
      .andExpect(jsonPath("authoritySourceFiles[0].metadata", notNullValue()))
      .andExpect(jsonPath("authoritySourceFiles[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("authoritySourceFiles[0].metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("authoritySourceFiles[0].metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("authoritySourceFiles[0].metadata.updatedByUserId", is(USER_ID)));
  }

  @ParameterizedTest
  @CsvSource({
    "0, 3, descending, name3",
    "1, 3, ascending, name2",
    "2, 2, descending, name1"
  })
  @DisplayName("Get Collection: return list of source files for the given limit and offset")
  void getCollection_positive_entitiesSortedByNameAndLimitedWithOffset(String offset, String limit, String sortOrder,
                                                                       String firstNoteTypeName) throws Exception {
    createAuthoritySourceTypes();

    var cqlQuery = "(cql.allRecords=1)sortby name/sort." + sortOrder;
    doGet(authoritySourceFilesEndpoint() + "?limit={l}&offset={o}&query={cql}", limit, offset, cqlQuery)
        .andExpect(jsonPath("authoritySourceFiles[0].name", is(firstNoteTypeName)))
        .andExpect(jsonPath("authoritySourceFiles[0].metadata.createdDate", notNullValue()))
        .andExpect(jsonPath("authoritySourceFiles[0].metadata.createdByUserId", is(USER_ID)))
        .andExpect(jsonPath("totalRecords").value(3));
  }

  // Tests for Get By ID

  @Test
  @DisplayName("Get By ID: return note type by given ID")
  void getById_positive_foundByIdForExistingEntity() throws Exception {
    var sourceFile = prepareAuthoritySourceFile(0);
    createAuthoritySourceFile(sourceFile);

    doGet(authoritySourceFilesEndpoint(sourceFile.getId()))
        .andExpect(jsonPath("name", is(sourceFile.getName())))
        .andExpect(jsonPath("metadata.createdDate", notNullValue()))
        .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)));
  }

  @Test
  @DisplayName("Get By ID: return 404 for not existing entity")
  void getById_negative_noAuthoritySourceFileExistForGivenId() throws Exception {

    tryGet(authoritySourceFilesEndpoint(UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(exceptionMatch(AuthoritySourceFileNotFoundException.class))
        .andExpect(errorMessageMatch(containsString("was not found")));
  }

  @Test
  @DisplayName("Get By ID: return 400 when id is invalid")
  void getById_negative_IdIsInvalid() throws Exception {
    tryGet(authoritySourceFilesEndpoint() + "/{id}", "invalid-uuid")
        .andExpect(status().isBadRequest())
        .andExpect(exceptionMatch(MethodArgumentTypeMismatchException.class))
        .andExpect(errorMessageMatch(containsString(
            "Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'")));
  }

  // Tests for POST

  @Test
  @DisplayName("POST: create new Authority Source File")
  void createAuthoritySourceFile_positive_entityCreated() throws Exception {
    assumeTrue(databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID) == 0);

    var dto = new AuthoritySourceFileDto("name", List.of("code"), "type", SourceEnum.FOLIO)
      .baseUrl("url");

    tryPost(authoritySourceFilesEndpoint(), dto)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("name", is(dto.getName())))
      .andExpect(jsonPath("source", is(dto.getSource().getValue())))
      .andExpect(jsonPath("codes", is(dto.getCodes())))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)));

    assertEquals(1, databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("POST: create new Authority Source File with existed name")
  void createAuthoritySourceFile_negative_existedName() throws Exception {
    var createdEntities = createAuthoritySourceTypes();

    var dto = new AuthoritySourceFileDto(createdEntities.get(0).getName(),
      List.of("code"), "type", SourceEnum.FOLIO).baseUrl("url");

    tryPost(authoritySourceFilesEndpoint(), dto)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is("Authority source file with the given 'name' already exists.")))
      .andExpect(exceptionMatch(DataIntegrityViolationException.class));

    assertEquals(createdEntities.size(),
      databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("POST: create new Authority Source File with existed url")
  void createAuthoritySourceFile_negative_existedUrl() throws Exception {
    var createdEntities = createAuthoritySourceTypes();

    var dto = new AuthoritySourceFileDto("new name",
      List.of("code"), "type", SourceEnum.FOLIO).baseUrl(createdEntities.get(0).getBaseUrl());

    tryPost(authoritySourceFilesEndpoint(), dto)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is("Authority source file with the given 'baseUrl' already exists.")))
      .andExpect(exceptionMatch(DataIntegrityViolationException.class));

    assertEquals(createdEntities.size(),
      databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("POST: create new Authority Source File with existed code")
  void createAuthoritySourceFile_negative_existedCode() throws Exception {
    var createdEntities = createAuthoritySourceTypes();

    var dto = new AuthoritySourceFileDto("new name",
      List.of("code1", "code5"), "type", SourceEnum.FOLIO).baseUrl("new url");

    tryPost(authoritySourceFilesEndpoint(), dto)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is("Authority source file with the given 'code' already exists.")))
      .andExpect(exceptionMatch(DataIntegrityViolationException.class));

    assertEquals(createdEntities.size(),
      databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("POST: return 422 for authority source file without name")
  void createAuthoritySourceFile_negative_entityWithoutNameNotCreated() throws Exception {
    var dto = new AuthoritySourceFileDto(null, List.of("code"), "type", SourceEnum.LOCAL);

    tryPost(authoritySourceFilesEndpoint(), dto)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(exceptionMatch(MethodArgumentNotValidException.class))
        .andExpect(jsonPath("$.errors.[0].parameters[0].key", is("name")))
        .andExpect(jsonPath("$.errors.[0].parameters[0].value", is("null")))
        .andExpect(errorMessageMatch(containsString("must not be null")));

  }

  // Tests for PATCH

  @Test
  @DisplayName("PATCH: partially update Authority Source File")
  void updateAuthoritySourceFilePartially_positive_entityGetUpdated() throws Exception {
    var dto = new AuthoritySourceFileDto("name", List.of("code1"), "type", SourceEnum.FOLIO)
      .baseUrl("url");

    doPost(authoritySourceFilesEndpoint(), dto);
    var existingAsString = doGet(authoritySourceFilesEndpoint()).andReturn().getResponse().getContentAsString();
    var collection = objectMapper.readValue(existingAsString, AuthoritySourceFileDtoCollection.class);
    var partiallyModified = collection.getAuthoritySourceFiles().get(0);
    // modify only source and codes fields, the rest should stay unchanged
    partiallyModified.setSource(SourceEnum.LOCAL);
    // remove code1 and insert code2 and code3
    partiallyModified.setCodes(List.of("code2", "code3"));
    partiallyModified.setName(null);
    partiallyModified.setType(null);
    partiallyModified.setBaseUrl(null);

    doPatch(authoritySourceFilesEndpoint(partiallyModified.getId()), partiallyModified)
      .andExpect(status().isNoContent());

    var content = doGet(authoritySourceFilesEndpoint(partiallyModified.getId()))
      .andExpect(jsonPath("source", is(partiallyModified.getSource().getValue())))
      .andExpect(jsonPath("codes", hasSize(2)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)))
      .andReturn().getResponse().getContentAsString();
    var resultDto = objectMapper.readValue(content, AuthoritySourceFileDto.class);

    assertThat(new HashSet<>(resultDto.getCodes()), equalTo(new HashSet<>(partiallyModified.getCodes())));
  }

  // Tests for DELETE

  @Test
  @DisplayName("DELETE: Should delete existing authority source file")
  void deleteAuthoritySourceFile_positive_deleteExistingEntity() {
    var noteType = prepareAuthoritySourceFile(0);
    createAuthoritySourceFile(noteType);

    doDelete(authoritySourceFilesEndpoint(noteType.getId()));

    assertEquals(0, databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID));
    assertEquals(0, databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("DELETE: Return 404 for non-existing entity")
  void deleteAuthoritySourceFile_negative_entityNotFound() throws Exception {

    tryDelete(authoritySourceFilesEndpoint(UUID.randomUUID())).andExpect(status().isNotFound())
        .andExpect(exceptionMatch(AuthoritySourceFileNotFoundException.class))
        .andExpect(errorMessageMatch(containsString("was not found")));
  }

  @Test
  @DisplayName("DELETE: Return 400 for invalid request id")
  void deleteAuthoritySourceFile_negative_invalidProvidedRequestId() throws Exception {

    tryDelete(authoritySourceFilesEndpoint() + "/{id}", "invalid-uuid").andExpect(status().isBadRequest())
        .andExpect(exceptionMatch(MethodArgumentTypeMismatchException.class))
        .andExpect(errorMessageMatch(containsString(
            "Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'")));
  }

  private List<AuthoritySourceFile> createAuthoritySourceTypes() {
    var sourceFile1 = prepareAuthoritySourceFile(0);
    var sourceFile2 = prepareAuthoritySourceFile(1);
    var sourceFile3 = prepareAuthoritySourceFile(2);

    createAuthoritySourceFile(sourceFile1);
    createAuthoritySourceFile(sourceFile2);
    createAuthoritySourceFile(sourceFile3);

    return List.of(sourceFile1, sourceFile2, sourceFile3);
  }

  private AuthoritySourceFile prepareAuthoritySourceFile(int i) {
    var entity = new AuthoritySourceFile();
    entity.setId(SOURCE_FILE_IDS[i]);
    entity.setName(SOURCE_FILE_NAMES[i]);
    entity.setSource(SOURCE_FILE_SOURCES[i].getValue());
    entity.setType(SOURCE_FILE_TYPES[i]);
    entity.setBaseUrl(SOURCE_FILE_URLS[i]  + "/");

    var code = prepareAuthoritySourceFileCode(i);
    entity.setCreatedDate(Timestamp.from(Instant.parse(CREATED_DATE)));
    entity.setCreatedByUserId(UUID.fromString(USER_ID));
    entity.setUpdatedDate(Timestamp.from(Instant.parse(CREATED_DATE)));
    entity.setUpdatedByUserId(UUID.fromString(USER_ID));
    entity.setAuthoritySourceFileCodes(Set.of(code));

    return entity;
  }

  private void createAuthoritySourceFile(AuthoritySourceFile entity) {
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, entity);
    createAuthoritySourceFileCode(entity);
  }

  private AuthoritySourceFileCode prepareAuthoritySourceFileCode(int i) {
    var code = new AuthoritySourceFileCode();
    code.setId(SOURCE_FILE_CODE_IDS[i]);
    code.setCode(SOURCE_FILE_CODES[i]);
    return code;
  }

  private void createAuthoritySourceFileCode(AuthoritySourceFile entity) {
    for (var code : entity.getAuthoritySourceFileCodes()) {
      databaseHelper.saveAuthoritySourceFileCode(TENANT_ID, entity.getId(), code);
    }
  }

  private ResultMatcher errorMessageMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].message", errorMessageMatcher);
  }
}
