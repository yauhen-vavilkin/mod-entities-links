package org.folio.entlinks.controller;

import static org.folio.entlinks.config.constants.ErrorCode.DISABLED_AUTO_LINKING;
import static org.folio.entlinks.config.constants.ErrorCode.MORE_THAN_ONE_SUGGESTIONS;
import static org.folio.entlinks.config.constants.ErrorCode.NO_SUGGESTIONS;
import static org.folio.entlinks.domain.dto.LinkStatus.ACTUAL;
import static org.folio.entlinks.domain.dto.LinkStatus.ERROR;
import static org.folio.entlinks.domain.dto.LinkStatus.NEW;
import static org.folio.support.JsonTestUtils.asJson;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.linksSuggestionsEndpoint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.domain.dto.AuthoritySearchParameter;
import org.folio.entlinks.domain.dto.FieldContent;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.ParsedRecordContent;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.AUTHORITY_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE})
class LinksSuggestionsIT extends IntegrationTestBase {

  private static final String BASE_URL = "id.loc.gov/authorities/names/";
  private static final String LINKABLE_AUTHORITY_ID = "417f3355-081c-4aae-9209-ccb305f25f7e";
  private static final String LINKABLE_AUTHORITY_WITH_T_SUBFIELD_ID = "cb398c08-736e-4d6b-aa94-5fa1bfcf9b10";
  private static final String NATURAL_ID = "oneAuthority";
  private static final String NATURAL_ID_FOR_T_SUBFIELD = "tSubfieldAuthority";

  @BeforeAll
  static void prepare() {
    setUpTenant();
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
    var sourceFileCode3 = new AuthoritySourceFileCode();
    sourceFileCode3.setAuthoritySourceFile(sourceFile);
    sourceFileCode3.setCode(NATURAL_ID_FOR_T_SUBFIELD.substring(0, 2));
    sourceFile.addCode(sourceFileCode2);
    sourceFile.addCode(sourceFileCode3);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, sourceFile);
    databaseHelper.saveAuthoritySourceFileCode(TENANT_ID, sourceFile.getId(), sourceFileCode1);
    databaseHelper.saveAuthoritySourceFileCode(TENANT_ID, sourceFile.getId(), sourceFileCode2);
    databaseHelper.saveAuthoritySourceFileCode(TENANT_ID, sourceFile.getId(), sourceFileCode3);
    var authority = TestDataUtils.AuthorityTestData.authority(0, 0);
    authority.setId(UUID.fromString(LINKABLE_AUTHORITY_ID));
    authority.setNaturalId(NATURAL_ID);
    databaseHelper.saveAuthority(TENANT_ID, authority);
    var authorityWithSubfieldT = TestDataUtils.AuthorityTestData.authority(0, 0);
    authorityWithSubfieldT.setId(UUID.fromString(LINKABLE_AUTHORITY_WITH_T_SUBFIELD_ID));
    authorityWithSubfieldT.setNaturalId(NATURAL_ID_FOR_T_SUBFIELD);
    databaseHelper.saveAuthority(TENANT_ID, authorityWithSubfieldT);
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldActualizeLinkAndSubfields() {
    var givenSubfields = Map.of("a", "old $a value", "0", BASE_URL + NATURAL_ID);
    var givenLinkDetails = getLinkDetails(ACTUAL, NATURAL_ID);
    var givenRecord = getRecord("100", givenLinkDetails, givenSubfields);

    var expectedLinkDetails = getLinkDetails(ACTUAL, NATURAL_ID);
    var expectedSubfields = Map.of("a", "new $a value", "0", BASE_URL + NATURAL_ID, "9", LINKABLE_AUTHORITY_ID);
    var expectedRecord = getRecord("100", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(), requestBody)
      .andExpect(status().isOk())
      .andExpect(content().json(asJson(new ParsedRecordContentCollection()
        .records(List.of(expectedRecord)), objectMapper)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldSuggestNewLink() {
    var givenSubfields = Map.of("0", NATURAL_ID);
    var givenRecord = getRecord("100", null, givenSubfields);

    var expectedLinkDetails = getLinkDetails(NEW, NATURAL_ID);
    var expectedSubfields = Map.of("a", "new $a value", "0", BASE_URL + NATURAL_ID, "9", LINKABLE_AUTHORITY_ID);
    var expectedRecord = getRecord("100", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(), requestBody)
      .andExpect(status().isOk())
      .andExpect(content().json(asJson(new ParsedRecordContentCollection()
        .records(List.of(expectedRecord)), objectMapper)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldSuggestNewLinkByAuthorityId() {
    var givenSubfields = Map.of("9", LINKABLE_AUTHORITY_ID);
    var givenRecord = getRecord("100", null, givenSubfields);

    var expectedLinkDetails = getLinkDetails(NEW, NATURAL_ID);
    var expectedSubfields = Map.of("a", "new $a value", "0", BASE_URL + NATURAL_ID, "9", LINKABLE_AUTHORITY_ID);
    var expectedRecord = getRecord("100", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(AuthoritySearchParameter.ID), requestBody)
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
    doPost(linksSuggestionsEndpoint(), requestBody)
      .andExpect(status().isOk())
      .andExpect(content().json(asJson(new ParsedRecordContentCollection()
        .records(List.of(expectedRecord)), objectMapper)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldFillErrorDetails_whenNoAuthoritiesFound() {
    var givenSubfields = Map.of("0", "noAuthority");
    var givenRecord = getRecord("100", null, givenSubfields);

    var expectedLinkDetails = new LinkDetails().status(ERROR).errorCause(NO_SUGGESTIONS.getCode());
    var expectedSubfields = Map.of("0", "noAuthority");
    var expectedRecord = getRecord("100", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(), requestBody)
      .andExpect(status().isOk())
      .andExpect(content().json(asJson(new ParsedRecordContentCollection()
        .records(List.of(expectedRecord)), objectMapper)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldFillErrorDetails_whenAutoLinkingDisabled() {
    var givenSubfields = Map.of("0", NATURAL_ID);
    var givenRecord = getRecord("100", null, givenSubfields);
    var disabledAutoLinkingRecord = getRecord("600", null, givenSubfields);

    var expectedErrorDetails = new LinkDetails().status(ERROR).errorCause(DISABLED_AUTO_LINKING.getCode());
    var expectedErrorRecord = getRecord("600", expectedErrorDetails, givenSubfields);

    var expectedLinkDetails = getLinkDetails(NEW, NATURAL_ID);
    var expectedSubfields = Map.of("a", "new $a value", "0", BASE_URL + NATURAL_ID, "9", LINKABLE_AUTHORITY_ID);
    var expectedRecord = getRecord("100", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord, disabledAutoLinkingRecord));
    doPost(linksSuggestionsEndpoint(), requestBody)
      .andExpect(status().isOk())
      .andExpect(content().json(asJson(new ParsedRecordContentCollection()
        .records(List.of(expectedRecord, expectedErrorRecord)), objectMapper)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldFillErrorDetails_whenTwoSuggestionsFound() {
    var naturalId = "twoAuthority";
    var authority1 = TestDataUtils.AuthorityTestData.authority(0, 0);
    authority1.setId(UUID.fromString("517f3355-081c-4aae-9209-ccb305f25f7e"));
    authority1.setNaturalId(naturalId);
    var authority2 = TestDataUtils.AuthorityTestData.authority(0, 0);
    authority2.setId(UUID.fromString("617f3355-081c-4aae-9209-ccb305f25f7e"));
    authority2.setNaturalId(naturalId);
    databaseHelper.saveAuthority(TENANT_ID, authority1);
    databaseHelper.saveAuthority(TENANT_ID, authority2);
    var givenSubfields = Map.of("0", naturalId);
    var givenRecord = getRecord("100", null, givenSubfields);

    var expectedLinkDetails = new LinkDetails().status(ERROR).errorCause(MORE_THAN_ONE_SUGGESTIONS.getCode());
    var expectedSubfields = Map.of("0", naturalId);
    var expectedRecord = getRecord("100", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(), requestBody)
      .andExpect(status().isOk())
      .andExpect(content().json(asJson(new ParsedRecordContentCollection()
        .records(List.of(expectedRecord)), objectMapper)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldSuggestNewLink_whenAutoLinkingIgnored() {
    var givenSubfields = Map.of("0", NATURAL_ID);
    var givenRecord = getRecord("600", null, givenSubfields);

    var expectedLinkDetails = getLinkDetails(NEW, NATURAL_ID, 8);
    var expectedSubfields = Map.of("a", "new $a value", "0", BASE_URL + NATURAL_ID, "9", LINKABLE_AUTHORITY_ID);
    var expectedRecord = getRecord("600", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(true), requestBody)
        .andExpect(status().isOk())
        .andExpect(content().json(asJson(new ParsedRecordContentCollection()
            .records(List.of(expectedRecord)), objectMapper)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldFillErrorDetails_whenAutoLinkingDisabled_andOnlyOneRecord() {
    var givenSubfields = Map.of("0", NATURAL_ID);
    var givenRecord = getRecord("600", null, givenSubfields);

    var expectedLinkDetails = new LinkDetails().status(ERROR).errorCause(DISABLED_AUTO_LINKING.getCode());
    var expectedRecord = getRecord("600", expectedLinkDetails, givenSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(), requestBody)
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

  private LinkDetails getLinkDetails(LinkStatus linkStatus, String naturalId) {
    return getLinkDetails(linkStatus, naturalId, 1);
  }

  private LinkDetails getLinkDetails(LinkStatus linkStatus, String naturalId, Integer linkingRuleId) {
    return getLinkDetails(LINKABLE_AUTHORITY_ID, linkStatus, naturalId, linkingRuleId);
  }

  private LinkDetails getLinkDetails(String authorityId, LinkStatus linkStatus, String naturalId,
                                     Integer linkingRuleId) {
    return new LinkDetails().linkingRuleId(linkingRuleId)
        .authorityId(UUID.fromString(authorityId))
        .authorityNaturalId(naturalId)
        .status(linkStatus);
  }
}
