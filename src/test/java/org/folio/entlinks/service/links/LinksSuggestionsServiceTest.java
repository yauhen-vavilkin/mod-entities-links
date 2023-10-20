package org.folio.entlinks.service.links;

import static java.util.Collections.emptyMap;
import static org.folio.entlinks.config.constants.ErrorCode.DISABLED_AUTO_LINKING;
import static org.folio.entlinks.config.constants.ErrorCode.MORE_THAN_ONE_SUGGESTIONS;
import static org.folio.entlinks.config.constants.ErrorCode.NO_SUGGESTIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.repository.AuthoritySourceFileCodeRepository;
import org.folio.entlinks.integration.dto.AuthorityParsedContent;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
  private static final String NATURAL_ID_SUBFIELD = "0";
  private static final String ID_SUBFIELD = "9";
  private static final String BASE_URL = "https://base/url/";
  private static final String SOURCE_FILE_NAME = "sourceFileName";

  private @Spy AuthorityRuleValidationService authorityRuleValidationService;
  private @Mock AuthoritySourceFileCodeRepository sourceFileCodeRepository;
  private @InjectMocks LinksSuggestionService linksSuggestionService;

  private AuthoritySourceFileCode sourceFileCode;

  @BeforeEach
  void setup() {
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setId(SOURCE_FILE_ID);
    sourceFile.setBaseUrl(BASE_URL);
    sourceFile.setName(SOURCE_FILE_NAME);
    sourceFileCode = new AuthoritySourceFileCode();
    sourceFileCode.setCode("e1");
    sourceFile.addCode(sourceFileCode);
  }

  @ParameterizedTest
  @ValueSource(strings = {NATURAL_ID_SUBFIELD, ID_SUBFIELD})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withNewLink(String linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", null);
    var authority = getAuthorityParsedRecordContent("100");
    when(sourceFileCodeRepository.findByCodeAsPrefixFor(anyString())).thenReturn(Optional.of(sourceFileCode));

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var bibField = bib.getFields().get(0);
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

  @ParameterizedTest
  @ValueSource(strings = {NATURAL_ID_SUBFIELD, ID_SUBFIELD})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withMultipleRulesForFieldAndOnlyOneSuitable(
      String linkingMatchSubfield) {
    var rules = getMapRule("240", List.of("100", "110", "111"));
    var bib = getBibParsedRecordContent("240", getActualLinksDetails());
    var authorityId = UUID.randomUUID();
    var authority = getAuthorityParsedRecordContent(UUID.randomUUID(), "130", Map.of("a", List.of("test")));
    var secondAuthority = getAuthorityParsedRecordContent(authorityId, "110", Map.of("a", List.of("test")));
    var thirdAuthority = getAuthorityParsedRecordContent(UUID.randomUUID(), "111", Map.of("a", List.of("test")));
    when(sourceFileCodeRepository.findByCodeAsPrefixFor(anyString())).thenReturn(Optional.of(sourceFileCode));

    linksSuggestionService
        .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority, secondAuthority, thirdAuthority),
            rules, linkingMatchSubfield, false);

    var bibField = bib.getFields().get(0);
    var linkDetails = bibField.getLinkDetails();
    assertEquals(LinkStatus.ACTUAL, linkDetails.getStatus());
    assertEquals(authorityId, linkDetails.getAuthorityId());
    assertEquals(NATURAL_ID, linkDetails.getAuthorityNaturalId());
    assertEquals(1, linkDetails.getLinkingRuleId());
    assertNull(linkDetails.getErrorCause());

    var bibSubfields = bibField.getSubfields();
    assertEquals(authorityId.toString(), bibSubfields.get("9").get(0));
    assertEquals(BASE_URL + NATURAL_ID, bibSubfields.get("0").get(0));
    assertFalse(bibSubfields.containsKey("a"));
    assertTrue(bibSubfields.containsKey("b"));
  }

  @ParameterizedTest
  @ValueSource(strings = {NATURAL_ID_SUBFIELD, ID_SUBFIELD})
  void fillLinkDetailsWithSuggestedAuthorities_shouldUpdateAndRemoveControlledSubfield(String linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var initialBibSubfields = new HashMap<String, List<String>>();
    initialBibSubfields.put("c", List.of("c value"));
    var bib = getBibParsedRecordContent("100", initialBibSubfields, null);
    var authority = getAuthorityParsedRecordContent("100");
    when(sourceFileCodeRepository.findByCodeAsPrefixFor(anyString())).thenReturn(Optional.of(sourceFileCode));

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var bibField = bib.getFields().get(0);
    var linkDetails = bibField.getLinkDetails();
    assertEquals(LinkStatus.NEW, linkDetails.getStatus());
    assertEquals(AUTHORITY_ID, linkDetails.getAuthorityId());
    assertEquals(NATURAL_ID, linkDetails.getAuthorityNaturalId());
    assertEquals(1, linkDetails.getLinkingRuleId());
    assertNull(linkDetails.getErrorCause());

    var bibSubfields = bibField.getSubfields();
    assertEquals(AUTHORITY_ID.toString(), bibSubfields.get("9").get(0));
    assertEquals(BASE_URL + NATURAL_ID, bibSubfields.get("0").get(0));
    assertFalse(bibSubfields.containsKey("c"));
    assertTrue(bibSubfields.containsKey("b"));
  }

  @ParameterizedTest
  @ValueSource(strings = {NATURAL_ID_SUBFIELD, ID_SUBFIELD})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withActualLink(String linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", getActualLinksDetails());
    var authority = getAuthorityParsedRecordContent("100");
    when(sourceFileCodeRepository.findByCodeAsPrefixFor(anyString())).thenReturn(Optional.of(sourceFileCode));

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var bibField = bib.getFields().get(0);
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

  @ParameterizedTest
  @ValueSource(strings = {NATURAL_ID_SUBFIELD, ID_SUBFIELD})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withError_whenRequiredAuthoritySubfieldNotExist(
    String linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", null);
    var authority = getAuthorityParsedRecordContent("100", emptyMap());

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var linkDetails = bib.getFields().get(0).getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getStatus());
    assertEquals(NO_SUGGESTIONS.getCode(), linkDetails.getErrorCause());
  }

  @ParameterizedTest
  @ValueSource(strings = {NATURAL_ID_SUBFIELD, ID_SUBFIELD})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withErrorMoreThenOneAuthoritiesFound(
    String linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", getActualLinksDetails());
    var authority = getAuthorityParsedRecordContent("100");
    var secondAuthority = getAuthorityParsedRecordContent("100");

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority, secondAuthority), rules,
        linkingMatchSubfield, false);

    var linkDetails = bib.getFields().get(0).getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getStatus());
    assertEquals(MORE_THAN_ONE_SUGGESTIONS.getCode(), linkDetails.getErrorCause());
    assertNull(linkDetails.getAuthorityId());
    assertNull(linkDetails.getAuthorityNaturalId());
    assertNull(linkDetails.getLinkingRuleId());
  }

  @ParameterizedTest
  @ValueSource(strings = {NATURAL_ID_SUBFIELD, ID_SUBFIELD})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withErrorNoAuthoritiesFound(
    String linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", null);
    var authority = getAuthorityParsedRecordContent("110");

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var linkDetails = bib.getFields().get(0).getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getStatus());
    assertEquals(NO_SUGGESTIONS.getCode(), linkDetails.getErrorCause());
  }

  @ParameterizedTest
  @ValueSource(strings = {NATURAL_ID_SUBFIELD, ID_SUBFIELD})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withErrorDisabledAutoLinkingFeature(
    String linkingMatchSubfield) {
    var rules = getMapRule("600", "100");
    disableAutoLinkingFeature(rules.get("600"));

    var bib = getBibParsedRecordContent("600", null);
    var authority = getAuthorityParsedRecordContent("110");

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var linkDetails = bib.getFields().get(0).getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getStatus());
    assertEquals(DISABLED_AUTO_LINKING.getCode(), linkDetails.getErrorCause());
  }

  @ParameterizedTest
  @ValueSource(strings = {NATURAL_ID_SUBFIELD, ID_SUBFIELD})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_onIgnoredAutoLinkingFeature(
    String linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    disableAutoLinkingFeature(rules.get("100"));
    var bib = getBibParsedRecordContent("100", getActualLinksDetails());
    var authority = getAuthorityParsedRecordContent("100");
    when(sourceFileCodeRepository.findByCodeAsPrefixFor(anyString())).thenReturn(Optional.of(sourceFileCode));

    linksSuggestionService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, true);

    var bibField = bib.getFields().get(0);
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
  void shouldFillErrorDetailsWithNoSuggestions_onlyForFieldWithNoError() {
    var bibs = List.of(getBibParsedRecordContent("100", getActualLinksDetails()),
        getBibParsedRecordContent("101", getActualLinksDetails().errorCause("test")));

    linksSuggestionService.fillErrorDetailsWithNoSuggestions(bibs, "0");

    assertEquals("101", bibs.get(0).getFields().get(0).getLinkDetails().getErrorCause());
    assertEquals("test", bibs.get(1).getFields().get(0).getLinkDetails().getErrorCause());
  }

  @Test
  void shouldFillErrorDetailsWithDisabledAutoLinking() {
    var field = new FieldParsedContent("100", "//", "//",
        Map.of(NATURAL_ID_SUBFIELD, List.of(NATURAL_ID)), null);

    linksSuggestionService.fillErrorDetailsWithDisabledAutoLinking(field, "0");

    assertEquals("103", field.getLinkDetails().getErrorCause());
  }

  @Test
  void shouldNotFillErrorDetailsWithDisabledAutoLinking_whenNoSubfield() {
    var field = new FieldParsedContent("100", "//", "//", Map.of(), null);

    linksSuggestionService.fillErrorDetailsWithDisabledAutoLinking(field, "0");

    assertNull(field.getLinkDetails());
  }

  private AuthorityParsedContent getAuthorityParsedRecordContent(String authorityField) {
    return getAuthorityParsedRecordContent(authorityField, Map.of("a", List.of("test")));
  }

  private AuthorityParsedContent getAuthorityParsedRecordContent(String authorityField,
                                                                 Map<String, List<String>> subfields) {
    return getAuthorityParsedRecordContent(AUTHORITY_ID, authorityField, subfields);
  }

  private AuthorityParsedContent getAuthorityParsedRecordContent(UUID authorityId, String authorityField,
                                                                 Map<String, List<String>> subfields) {
    var field = new FieldParsedContent(authorityField, "//", "//", subfields, null);
    return new AuthorityParsedContent(authorityId, NATURAL_ID, "", List.of(field));
  }

  private SourceParsedContent getBibParsedRecordContent(String bibField, LinkDetails linkDetails) {
    return getBibParsedRecordContent(bibField, new HashMap<>(), linkDetails);
  }

  private SourceParsedContent getBibParsedRecordContent(String bibField, Map<String, List<String>> subfields,
                                                        LinkDetails linkDetails) {
    subfields.put(NATURAL_ID_SUBFIELD, List.of(NATURAL_ID));
    subfields.put(ID_SUBFIELD, List.of(AUTHORITY_ID.toString()));
    var field = new FieldParsedContent(bibField, "//", "//", subfields, linkDetails);
    return new SourceParsedContent(UUID.randomUUID(), "", List.of(field));
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
    rule.setAuthoritySubfields(new char[] {'a', 'c'});
    rule.setSubfieldModifications(List.of(modification));
    rule.setSubfieldsExistenceValidations(existence);

    return Map.of(bibField, List.of(rule));
  }

  private Map<String, List<InstanceAuthorityLinkingRule>> getMapRule(String bibField, List<String> authorityFields) {
    var modification = new SubfieldModification().source("a").target("b");
    var existence = Map.of("a", true);

    var rules = new ArrayList<InstanceAuthorityLinkingRule>();
    for (String authorityField : authorityFields) {
      var rule = new InstanceAuthorityLinkingRule();
      rule.setId(1);
      rule.setBibField(bibField);
      rule.setAuthorityField(authorityField);
      rule.setAutoLinkingEnabled(true);
      rule.setAuthoritySubfields(new char[]{'a', 'c'});
      rule.setSubfieldModifications(List.of(modification));
      rule.setSubfieldsExistenceValidations(existence);
      rules.add(rule);
    }

    return Map.of(bibField, rules);
  }


  private void disableAutoLinkingFeature(List<InstanceAuthorityLinkingRule> rules) {
    rules.forEach(rule -> rule.setAutoLinkingEnabled(false));
  }
}
