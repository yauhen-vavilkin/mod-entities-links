package org.folio.entlinks.controller;

import static org.folio.entlinks.domain.dto.LinkStatus.ACTUAL;
import static org.folio.entlinks.domain.dto.LinkStatus.ERROR;
import static org.folio.entlinks.domain.dto.LinkStatus.NEW;
import static org.folio.support.JsonTestUtils.asJson;
import static org.folio.support.base.TestConstants.linksSuggestionsEndpoint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.domain.dto.FieldContent;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.ParsedRecordContent;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.Test;

@IntegrationTest
class LinksSuggestionsIT extends IntegrationTestBase {

  private static final String BASE_URL = "id.loc.gov/authorities/names/";
  private static final String LINKABLE_AUTHORITY_ID = "417f3355-081c-4aae-9209-ccb305f25f7e";
  private static final String MORE_THEN_ONE_SUGGESTION_ERROR_CODE = "102";
  private static final String NO_SUGGESTIONS_ERROR_CODE = "101";

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldActualizeLinkAndSubfields() {
    var givenSubfields = Map.of("a", "old $a value", "0", BASE_URL + "oneAuthority");
    var givenLinkDetails = getLinkDetails(ACTUAL, "oneAuthority");
    var givenRecord = getRecord("100", givenLinkDetails, givenSubfields);

    var expectedLinkDetails = getLinkDetails(ACTUAL, "oneAuthority");
    var expectedSubfields = Map.of("a", "new $a value", "0", BASE_URL + "oneAuthority", "9", LINKABLE_AUTHORITY_ID);
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
    var givenSubfields = Map.of("0", "oneAuthority");
    var givenRecord = getRecord("100", null, givenSubfields);

    var expectedLinkDetails = getLinkDetails(NEW, "oneAuthority");
    var expectedSubfields = Map.of("a", "new $a value", "0", BASE_URL + "oneAuthority", "9", LINKABLE_AUTHORITY_ID);
    var expectedRecord = getRecord("100", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(), requestBody)
      .andExpect(status().isOk())
      .andExpect(content().json(asJson(new ParsedRecordContentCollection()
        .records(List.of(expectedRecord)), objectMapper)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldFillErrorDetails_whenNoSuggestionsFound() {
    var givenSubfields = Map.of("0", "oneAuthority");
    var givenRecord = getRecord("110", null, givenSubfields);

    var expectedLinkDetails = new LinkDetails().linksStatus(ERROR).errorStatusCode(NO_SUGGESTIONS_ERROR_CODE);
    var expectedSubfields = Map.of("0", "oneAuthority");
    var expectedRecord = getRecord("110", expectedLinkDetails, expectedSubfields);

    var requestBody = new ParsedRecordContentCollection().records(List.of(givenRecord));
    doPost(linksSuggestionsEndpoint(), requestBody)
      .andExpect(status().isOk())
      .andExpect(content().json(asJson(new ParsedRecordContentCollection()
        .records(List.of(expectedRecord)), objectMapper)));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_shouldFillErrorDetails_whenTwoSuggestionsFound() {
    var givenSubfields = Map.of("0", "twoAuthority");
    var givenRecord = getRecord("100", null, givenSubfields);

    var expectedLinkDetails = new LinkDetails().linksStatus(ERROR).errorStatusCode(MORE_THEN_ONE_SUGGESTION_ERROR_CODE);
    var expectedSubfields = Map.of("0", "twoAuthority");
    var expectedRecord = getRecord("100", expectedLinkDetails, expectedSubfields);

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
    return new LinkDetails().ruleId(1)
      .authorityId(UUID.fromString(LINKABLE_AUTHORITY_ID))
      .naturalId(naturalId)
      .linksStatus(linkStatus);
  }
}
