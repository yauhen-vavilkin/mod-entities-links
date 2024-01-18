package org.folio.entlinks.controller;

import static org.folio.entlinks.config.constants.ErrorCode.NO_SUGGESTIONS;
import static org.folio.entlinks.controller.ConsortiumLinksSuggestionsIT.COLLEGE_TENANT_ID;
import static org.folio.entlinks.controller.ConsortiumLinksSuggestionsIT.UNIVERSITY_TENANT_ID;
import static org.folio.entlinks.domain.dto.LinkStatus.ACTUAL;
import static org.folio.entlinks.domain.dto.LinkStatus.ERROR;
import static org.folio.entlinks.domain.dto.LinkStatus.NEW;
import static org.folio.support.DatabaseHelper.AUTHORITY_TABLE;
import static org.folio.support.JsonTestUtils.asJson;
import static org.folio.support.base.TestConstants.CENTRAL_TENANT_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.linksSuggestionsEndpoint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthoritySearchParameter;
import org.folio.entlinks.domain.dto.FieldContent;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.ParsedRecordContent;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

@IntegrationTest
@DatabaseCleanup(tables = {DatabaseHelper.AUTHORITY_TABLE, DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE,
                           DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE},
                 tenants = {CENTRAL_TENANT_ID, COLLEGE_TENANT_ID, UNIVERSITY_TENANT_ID
                 })
class ConsortiumLinksSuggestionsIT extends IntegrationTestBase {

  public static final String COLLEGE_TENANT_ID = "college";
  public static final String UNIVERSITY_TENANT_ID = "university";

  private static final String BASE_URL = "http://id.loc.gov/authorities/names/";
  private static final String LINKABLE_AUTHORITY_ID = "417f3355-081c-4aae-9209-ccb305f25f7e";
  private static final String NATURAL_ID = "oneAuthority";

  @BeforeAll
  static void prepare() {
    setUpConsortium(CENTRAL_TENANT_ID, List.of(COLLEGE_TENANT_ID, UNIVERSITY_TENANT_ID), true);
  }

  @BeforeEach
  public void setup() {
    var sourceFile = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    sourceFile.setBaseUrl(BASE_URL);
    var sourceFileCode1 = sourceFile.getAuthoritySourceFileCodes().iterator().next();
    var sourceFileCode2 = new AuthoritySourceFileCode();
    sourceFileCode1.setCode(NATURAL_ID.substring(0, 3));
    sourceFileCode2.setAuthoritySourceFile(sourceFile);
    sourceFileCode2.setCode(NATURAL_ID.substring(0, 2));
    sourceFile.addCode(sourceFileCode2);
    databaseHelper.saveAuthoritySourceFile(CENTRAL_TENANT_ID, sourceFile);
    databaseHelper.saveAuthoritySourceFileCode(CENTRAL_TENANT_ID, sourceFile.getId(), sourceFileCode1);
    databaseHelper.saveAuthoritySourceFileCode(CENTRAL_TENANT_ID, sourceFile.getId(), sourceFileCode2);
    databaseHelper.saveAuthoritySourceFile(COLLEGE_TENANT_ID, sourceFile);
    databaseHelper.saveAuthoritySourceFileCode(COLLEGE_TENANT_ID, sourceFile.getId(), sourceFileCode1);
    databaseHelper.saveAuthoritySourceFileCode(COLLEGE_TENANT_ID, sourceFile.getId(), sourceFileCode2);
    databaseHelper.saveAuthoritySourceFile(UNIVERSITY_TENANT_ID, sourceFile);
    databaseHelper.saveAuthoritySourceFileCode(UNIVERSITY_TENANT_ID, sourceFile.getId(), sourceFileCode1);
    databaseHelper.saveAuthoritySourceFileCode(UNIVERSITY_TENANT_ID, sourceFile.getId(), sourceFileCode2);

    var authorityDto = new AuthorityDto()
        .id(UUID.fromString(LINKABLE_AUTHORITY_ID))
        .sourceFileId(sourceFile.getId())
        .naturalId(NATURAL_ID)
        .source("MARC")
        .personalName("Personal Name")
        .subjectHeadings("a");
    doPost(authorityEndpoint(), authorityDto, tenantHeaders(CENTRAL_TENANT_ID));
    assertEquals(1, databaseHelper.countRows(AUTHORITY_TABLE, CENTRAL_TENANT_ID));
    awaitUntilAsserted(() ->
        assertEquals(1, databaseHelper.countRows(AUTHORITY_TABLE, COLLEGE_TENANT_ID)));
    awaitUntilAsserted(() ->
        assertEquals(1, databaseHelper.countRows(AUTHORITY_TABLE, UNIVERSITY_TENANT_ID)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldActualizeLinkAndSubfieldsForMemberTenant() {
    var givenSubfields = Map.of("a", "old $a value", "0", BASE_URL + NATURAL_ID);
    var givenLinkDetails = getLinkDetails(ACTUAL);
    var givenRecord = getRecord("100", givenLinkDetails, givenSubfields);

    var expectedLinkDetails = getLinkDetails(ACTUAL);
    var expectedSubfields = Map.of("a", "new $a value", "0", BASE_URL + NATURAL_ID, "9", LINKABLE_AUTHORITY_ID);
    var expectedRecord = getRecord("100", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(), requestBody, tenantHeaders(COLLEGE_TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().json(asJson(new ParsedRecordContentCollection()
        .records(List.of(expectedRecord)), objectMapper)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldSuggestNewLinkForMemberTenant() {
    var givenSubfields = Map.of("0", NATURAL_ID);
    var givenRecord = getRecord("100", null, givenSubfields);

    var expectedLinkDetails = getLinkDetails(NEW);
    var expectedSubfields = Map.of("a", "new $a value", "0", BASE_URL + NATURAL_ID, "9", LINKABLE_AUTHORITY_ID);
    var expectedRecord = getRecord("100", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(), requestBody, tenantHeaders(UNIVERSITY_TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().json(asJson(new ParsedRecordContentCollection()
        .records(List.of(expectedRecord)), objectMapper)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldSuggestNewLinkByAuthorityIdForMemberTenant() {
    var givenSubfields = Map.of("9", LINKABLE_AUTHORITY_ID);
    var givenRecord = getRecord("100", null, givenSubfields);

    var expectedLinkDetails = getLinkDetails(NEW);
    var expectedSubfields = Map.of("a", "new $a value", "0", BASE_URL + NATURAL_ID, "9", LINKABLE_AUTHORITY_ID);
    var expectedRecord = getRecord("100", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(AuthoritySearchParameter.ID), requestBody, tenantHeaders(UNIVERSITY_TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().json(asJson(new ParsedRecordContentCollection()
        .records(List.of(expectedRecord)), objectMapper)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldFillErrorDetails_whenNoSuggestionsFound() {
    var givenSubfields = Map.of("0", NATURAL_ID);
    var givenRecord = getRecord("110", null, givenSubfields);

    var expectedLinkDetails = new LinkDetails().status(ERROR).errorCause(NO_SUGGESTIONS.getCode());
    var expectedSubfields = Map.of("0", NATURAL_ID);
    var expectedRecord = getRecord("110", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(), requestBody, tenantHeaders(COLLEGE_TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().json(asJson(new ParsedRecordContentCollection()
        .records(List.of(expectedRecord)), objectMapper)));
  }

  private ParsedRecordContent getRecord(String bibField, LinkDetails linkDetails, Map<String, String> subfields) {
    var field = new FieldContent();
    field.setLinkDetails(linkDetails);

    subfields.forEach((key, value) -> field.addSubfieldsItem(Map.of(key, value)));

    var fields = Map.of(bibField, field);
    return new ParsedRecordContent(List.of(fields), "default leader");
  }

  private LinkDetails getLinkDetails(LinkStatus linkStatus) {
    return new LinkDetails().linkingRuleId(1)
        .authorityId(UUID.fromString(LINKABLE_AUTHORITY_ID))
        .authorityNaturalId(NATURAL_ID)
        .status(linkStatus);
  }

  private HttpHeaders tenantHeaders(String tenant) {
    var httpHeaders = defaultHeaders();
    httpHeaders.put(XOkapiHeaders.TENANT, Collections.singletonList(tenant));
    return httpHeaders;
  }
}
