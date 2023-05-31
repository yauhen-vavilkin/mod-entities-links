package org.folio.entlinks.service.links;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.client.AuthoritySourceFileClient.AuthoritySourceFile;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.integration.dto.AuthorityParsedContent;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.folio.entlinks.integration.internal.AuthoritySourceFilesService;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinksSuggestionsServiceTest {

  private static final UUID AUTHORITY_ID = UUID.randomUUID();
  private static final UUID SOURCE_FILE_ID = UUID.randomUUID();
  private static final String NO_SUGGESTIONS_ERROR_CODE = "101";
  private static final String MORE_THEN_ONE_SUGGESTIONS_ERROR_CODE = "102";
  private static final String NATURAL_ID = "e12345";
  private static final String BASE_URL = "https://base/url/";
  private static final String SOURCE_FILE_NAME = "sourceFileName";

  private @Mock AuthoritySourceFilesService sourceFilesService;
  private @InjectMocks LinksSuggestionService linksSuggestionService;

  @Test
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withNewLink() {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", null);
    var authority = getAuthorityParsedRecordContent("100");
    var sourceFile = new AuthoritySourceFile(SOURCE_FILE_ID, BASE_URL, SOURCE_FILE_NAME, codes("e1"));

    when(sourceFilesService.findAuthoritySourceFileByNaturalId(anyMap(), eq(NATURAL_ID))).thenReturn(sourceFile);

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules);

    var bibField = bib.getFields().get("100");
    var linkDetails = bibField.getLinkDetails();
    assertEquals(LinkStatus.NEW, linkDetails.getLinksStatus());
    assertEquals(AUTHORITY_ID, linkDetails.getAuthorityId());
    assertEquals(NATURAL_ID, linkDetails.getNaturalId());
    assertEquals(1, linkDetails.getRuleId());
    assertNull(linkDetails.getErrorStatusCode());

    var bibSubfields = bibField.getSubfields();
    assertEquals(AUTHORITY_ID.toString(), bibSubfields.get("9"));
    assertEquals(BASE_URL + NATURAL_ID, bibSubfields.get("0"));
    assertFalse(bibSubfields.containsKey("a"));
    assertTrue(bibSubfields.containsKey("b"));
  }

  @Test
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withActualLink() {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", getActualLinksDetails());
    var authority = getAuthorityParsedRecordContent("100");
    var sourceFile = new AuthoritySourceFile(SOURCE_FILE_ID, BASE_URL, SOURCE_FILE_NAME, codes("e1"));

    when(sourceFilesService.findAuthoritySourceFileByNaturalId(anyMap(), eq(NATURAL_ID))).thenReturn(sourceFile);

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules);

    var bibField = bib.getFields().get("100");
    var linkDetails = bibField.getLinkDetails();
    assertEquals(LinkStatus.ACTUAL, linkDetails.getLinksStatus());
    assertEquals(AUTHORITY_ID, linkDetails.getAuthorityId());
    assertEquals(NATURAL_ID, linkDetails.getNaturalId());
    assertEquals(1, linkDetails.getRuleId());
    assertNull(linkDetails.getErrorStatusCode());

    var bibSubfields = bibField.getSubfields();
    assertEquals(AUTHORITY_ID.toString(), bibSubfields.get("9"));
    assertEquals(BASE_URL + NATURAL_ID, bibSubfields.get("0"));
    assertFalse(bibSubfields.containsKey("a"));
    assertTrue(bibSubfields.containsKey("b"));
  }

  @Test
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withError_whenRequiredAuthoritySubfieldNotExist() {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", null);
    var authority = getAuthorityParsedRecordContent("100", emptyMap());

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules);

    var linkDetails = bib.getFields().get("100").getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getLinksStatus());
    assertEquals(NO_SUGGESTIONS_ERROR_CODE, linkDetails.getErrorStatusCode());
  }

  @Test
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withErrorMoreThenOneAuthoritiesFound() {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", getActualLinksDetails());
    var authority = getAuthorityParsedRecordContent("100");
    var secondAuthority = getAuthorityParsedRecordContent("100");

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority, secondAuthority), rules);

    var linkDetails = bib.getFields().get("100").getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getLinksStatus());
    assertEquals(MORE_THEN_ONE_SUGGESTIONS_ERROR_CODE, linkDetails.getErrorStatusCode());
    assertNull(linkDetails.getAuthorityId());
    assertNull(linkDetails.getNaturalId());
    assertNull(linkDetails.getRuleId());
  }

  @Test
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withErrorNoAuthoritiesFound() {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", null);
    var authority = getAuthorityParsedRecordContent("110");

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules);

    var linkDetails = bib.getFields().get("100").getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getLinksStatus());
    assertEquals(NO_SUGGESTIONS_ERROR_CODE, linkDetails.getErrorStatusCode());
  }

  private AuthorityParsedContent getAuthorityParsedRecordContent(String authorityField) {
    return getAuthorityParsedRecordContent(authorityField, Map.of("a", "test"));
  }

  private AuthorityParsedContent getAuthorityParsedRecordContent(String authorityField, Map<String, String> subfields) {
    var field = new FieldParsedContent("//", "//", subfields, null);
    var fields = Map.of(authorityField, field);
    return new AuthorityParsedContent(AUTHORITY_ID, NATURAL_ID, "", fields);
  }

  private SourceParsedContent getBibParsedRecordContent(String bibField, LinkDetails linkDetails) {
    var field = new FieldParsedContent("//", "//", new HashMap<>(), linkDetails);
    var fields = Map.of(bibField, field);
    return new SourceParsedContent(UUID.randomUUID(), "", fields);
  }

  private LinkDetails getActualLinksDetails() {
    return new LinkDetails().ruleId(2)
      .linksStatus(LinkStatus.ACTUAL)
      .authorityId(UUID.randomUUID())
      .naturalId(NATURAL_ID);
  }

  private Map<String, List<InstanceAuthorityLinkingRule>> getMapRule(String bibField, String authorityField) {
    var modification = new SubfieldModification().source("a").target("b");
    var existence = Map.of("a", true);

    var rule = new InstanceAuthorityLinkingRule();
    rule.setId(1);
    rule.setBibField(bibField);
    rule.setAuthorityField(authorityField);
    rule.setAutoLinkingEnabled(true);
    rule.setAuthoritySubfields(new char[] {'a', 'b'});
    rule.setSubfieldModifications(List.of(modification));
    rule.setSubfieldsExistenceValidations(existence);

    return Map.of(bibField, List.of(rule));
  }

  private List<String> codes(String... codes) {
    return List.of(codes);
  }
}
