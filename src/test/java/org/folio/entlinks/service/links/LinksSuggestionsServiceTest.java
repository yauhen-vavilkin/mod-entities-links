package org.folio.entlinks.service.links;

import static java.util.Collections.emptyMap;
import static org.folio.entlinks.service.links.model.LinksSuggestionErrorCode.DISABLED_AUTO_LINKING;
import static org.folio.entlinks.service.links.model.LinksSuggestionErrorCode.MORE_THAN_ONE_SUGGESTIONS;
import static org.folio.entlinks.service.links.model.LinksSuggestionErrorCode.NO_SUGGESTIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinksSuggestionsServiceTest {

  private static final UUID AUTHORITY_ID = UUID.randomUUID();
  private static final UUID SOURCE_FILE_ID = UUID.randomUUID();
  private static final String NATURAL_ID = "e12345";
  private static final String BASE_URL = "https://base/url/";
  private static final String SOURCE_FILE_NAME = "sourceFileName";

  private @Spy AuthorityRuleValidationService authorityRuleValidationService;
  private @Mock AuthoritySourceFilesService sourceFilesService;
  private @InjectMocks LinksSuggestionService linksSuggestionService;

  @Test
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withNewLink() {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", null);
    var authority = getAuthorityParsedRecordContent("100");
    var sourceFile = new AuthoritySourceFile(SOURCE_FILE_ID, BASE_URL, SOURCE_FILE_NAME, codes("e1"));

    when(sourceFilesService.fetchAuthoritySources()).thenReturn(Map.of(sourceFile.id(), sourceFile));

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules);

    var bibField = bib.getFields().get("100").get(0);
    var linkDetails = bibField.getLinkDetails();
    assertEquals(LinkStatus.NEW, linkDetails.getStatus());
    assertEquals(AUTHORITY_ID, linkDetails.getAuthorityId());
    assertEquals(NATURAL_ID, linkDetails.getAuthorityNaturalId());
    assertEquals(1, linkDetails.getLinkingRuleId());
    assertNull(linkDetails.getErrorCause());

    var bibSubfields = bibField.getSubfields();
    assertEquals(AUTHORITY_ID.toString(), bibSubfields.get("9").get(0));
    assertEquals(BASE_URL + NATURAL_ID, bibSubfields.get("0").get(0));
    assertFalse(bibSubfields.containsKey("a"));
    assertTrue(bibSubfields.containsKey("b"));
  }

  @Test
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withActualLink() {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", getActualLinksDetails());
    var authority = getAuthorityParsedRecordContent("100");
    var sourceFile = new AuthoritySourceFile(SOURCE_FILE_ID, BASE_URL, SOURCE_FILE_NAME, codes("e1"));

    when(sourceFilesService.fetchAuthoritySources()).thenReturn(Map.of(sourceFile.id(), sourceFile));

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules);

    var bibField = bib.getFields().get("100").get(0);
    var linkDetails = bibField.getLinkDetails();
    assertEquals(LinkStatus.ACTUAL, linkDetails.getStatus());
    assertEquals(AUTHORITY_ID, linkDetails.getAuthorityId());
    assertEquals(NATURAL_ID, linkDetails.getAuthorityNaturalId());
    assertEquals(1, linkDetails.getLinkingRuleId());
    assertNull(linkDetails.getErrorCause());

    var bibSubfields = bibField.getSubfields();
    assertEquals(AUTHORITY_ID.toString(), bibSubfields.get("9").get(0));
    assertEquals(BASE_URL + NATURAL_ID, bibSubfields.get("0").get(0));
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

    var linkDetails = bib.getFields().get("100").get(0).getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getStatus());
    assertEquals(NO_SUGGESTIONS.getErrorCode(), linkDetails.getErrorCause());
  }

  @Test
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withErrorMoreThenOneAuthoritiesFound() {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", getActualLinksDetails());
    var authority = getAuthorityParsedRecordContent("100");
    var secondAuthority = getAuthorityParsedRecordContent("100");

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority, secondAuthority), rules);

    var linkDetails = bib.getFields().get("100").get(0).getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getStatus());
    assertEquals(MORE_THAN_ONE_SUGGESTIONS.getErrorCode(), linkDetails.getErrorCause());
    assertNull(linkDetails.getAuthorityId());
    assertNull(linkDetails.getAuthorityNaturalId());
    assertNull(linkDetails.getLinkingRuleId());
  }

  @Test
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withErrorNoAuthoritiesFound() {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", null);
    var authority = getAuthorityParsedRecordContent("110");

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules);

    var linkDetails = bib.getFields().get("100").get(0).getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getStatus());
    assertEquals(NO_SUGGESTIONS.getErrorCode(), linkDetails.getErrorCause());
  }

  @Test
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withErrorDisabledAutoLinkingFeature() {
    var rules = getMapRule("600", "100");
    disableAutoLinkingFeature(rules.get("600"));

    var bib = getBibParsedRecordContent("600", null);
    var authority = getAuthorityParsedRecordContent("110");

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules);

    var linkDetails = bib.getFields().get("600").get(0).getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getStatus());
    assertEquals(DISABLED_AUTO_LINKING.getErrorCode(), linkDetails.getErrorCause());
  }

  private AuthorityParsedContent getAuthorityParsedRecordContent(String authorityField) {
    return getAuthorityParsedRecordContent(authorityField, Map.of("a", List.of("test")));
  }

  private AuthorityParsedContent getAuthorityParsedRecordContent(String authorityField,
                                                                 Map<String, List<String>> subfields) {
    var field = new FieldParsedContent("//", "//", subfields, null);
    var fields = Map.of(authorityField, List.of(field));
    return new AuthorityParsedContent(AUTHORITY_ID, NATURAL_ID, "", fields);
  }

  private SourceParsedContent getBibParsedRecordContent(String bibField, LinkDetails linkDetails) {
    var subfields = new HashMap<String, List<String>>();
    subfields.put("0", List.of(NATURAL_ID));
    var field = new FieldParsedContent("//", "//", subfields, linkDetails);
    var fields = Map.of(bibField, List.of(field));
    return new SourceParsedContent(UUID.randomUUID(), "", fields);
  }

  private LinkDetails getActualLinksDetails() {
    return new LinkDetails().linkingRuleId(2)
      .status(LinkStatus.ACTUAL)
      .authorityId(UUID.randomUUID())
      .authorityNaturalId(NATURAL_ID);
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

  private void disableAutoLinkingFeature(List<InstanceAuthorityLinkingRule> rules) {
    rules.forEach(rule -> rule.setAutoLinkingEnabled(false));
  }

  private List<String> codes(String... codes) {
    return List.of(codes);
  }
}
