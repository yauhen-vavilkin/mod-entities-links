package org.folio.entlinks.controller;

import static org.folio.entlinks.domain.dto.LinkStatus.ACTUAL;
import static org.folio.entlinks.domain.dto.LinkStatus.ERROR;
import static org.folio.entlinks.domain.dto.LinkStatus.NEW;
import static org.folio.entlinks.service.links.model.LinksSuggestionErrorCode.DISABLED_AUTO_LINKING;
import static org.folio.entlinks.service.links.model.LinksSuggestionErrorCode.MORE_THAN_ONE_SUGGESTIONS;
import static org.folio.entlinks.service.links.model.LinksSuggestionErrorCode.NO_SUGGESTIONS;
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

    var expectedLinkDetails = new LinkDetails().status(ERROR).errorCause(NO_SUGGESTIONS.getErrorCode());
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
  void getAuthDataStat_shouldFillErrorDetails_whenNoAuthoritiesFound() {
    var givenSubfields = Map.of("0", "noAuthority");
    var givenRecord = getRecord("100", null, givenSubfields);

    var expectedLinkDetails = new LinkDetails().status(ERROR).errorCause(NO_SUGGESTIONS.getErrorCode());
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
    var givenSubfields = Map.of("0", "oneAuthority");
    var givenRecord = getRecord("100", null, givenSubfields);
    var disabledAutoLinkingRecord = getRecord("600", null, givenSubfields);

    var expectedErrorDetails = new LinkDetails().status(ERROR).errorCause(DISABLED_AUTO_LINKING.getErrorCode());
    var expectedErrorRecord = getRecord("600", expectedErrorDetails, givenSubfields);

    var expectedLinkDetails = getLinkDetails(NEW, "oneAuthority");
    var expectedSubfields = Map.of("a", "new $a value", "0", BASE_URL + "oneAuthority", "9", LINKABLE_AUTHORITY_ID);
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
    var givenSubfields = Map.of("0", "twoAuthority");
    var givenRecord = getRecord("100", null, givenSubfields);

    var expectedLinkDetails = new LinkDetails().status(ERROR).errorCause(MORE_THAN_ONE_SUGGESTIONS.getErrorCode());
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
    return new LinkDetails().linkingRuleId(1)
      .authorityId(UUID.fromString(LINKABLE_AUTHORITY_ID))
      .authorityNaturalId(naturalId)
      .status(linkStatus);
  }
}
