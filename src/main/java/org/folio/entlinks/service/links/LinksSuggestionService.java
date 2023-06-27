package org.folio.entlinks.service.links;

import static java.util.Objects.isNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.folio.entlinks.config.constants.ErrorCode.DISABLED_AUTO_LINKING;
import static org.folio.entlinks.config.constants.ErrorCode.MORE_THAN_ONE_SUGGESTIONS;
import static org.folio.entlinks.config.constants.ErrorCode.NO_SUGGESTIONS;
import static org.folio.entlinks.domain.dto.LinkStatus.ERROR;
import static org.folio.entlinks.domain.dto.LinkStatus.NEW;
import static org.folio.entlinks.utils.FieldUtils.getSubfield0Value;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.config.constants.ErrorCode;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.integration.dto.AuthorityParsedContent;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.folio.entlinks.integration.internal.AuthoritySourceFilesService;
import org.folio.entlinks.utils.FieldUtils;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class LinksSuggestionService {

  private final AuthoritySourceFilesService sourceFilesService;
  private final AuthorityRuleValidationService authorityRuleValidationService;

  /**
   * Validate bib-authority fields by linking rules and fill bib fields with suggested links.
   *
   * @param marcBibsContent        list of bib records {@link SourceParsedContent}
   * @param marcAuthoritiesContent list of authorities {@link AuthorityParsedContent} that can be suggested as link
   * @param rules                  linking rules
   *                               <p>Key - bib tag, Value - list of {@link InstanceAuthorityLinkingRule}</p>
   * @param linkingMatchSubfield   bib field's subfield code to use as a matching point for links
   */
  public void fillLinkDetailsWithSuggestedAuthorities(List<SourceParsedContent> marcBibsContent,
                                                      List<AuthorityParsedContent> marcAuthoritiesContent,
                                                      Map<String, List<InstanceAuthorityLinkingRule>> rules,
                                                      String linkingMatchSubfield) {
    marcBibsContent.stream()
      .flatMap(bibContent -> bibContent.getFields().stream())
      .forEach(bibField -> suggestAuthorityForBibFields(
        List.of(bibField), marcAuthoritiesContent, rules.get(bibField.getTag()),
        linkingMatchSubfield
      ));
  }

  /**
   * Fill bib fields with no suggestions error detail, if it contains subfields $0.
   *
   * @param marcBibsContent list of bib records {@link SourceParsedContent}
   */
  public void fillErrorDetailsWithNoSuggestions(List<SourceParsedContent> marcBibsContent,
                                                String linkingMatchSubfield) {
    marcBibsContent.stream()
      .flatMap(bibContent -> bibContent.getFields().stream())
      .filter(fieldContent -> containsSubfield(fieldContent, linkingMatchSubfield))
      .forEach(bibField -> bibField.setLinkDetails(getErrorDetails(NO_SUGGESTIONS)));
  }

  private void suggestAuthorityForBibFields(List<FieldParsedContent> bibFields,
                                            List<AuthorityParsedContent> marcAuthoritiesContent,
                                            List<InstanceAuthorityLinkingRule> rules,
                                            String linkingMatchSubfield) {
    if (isNotEmpty(rules) && isNotEmpty(bibFields)) {
      for (InstanceAuthorityLinkingRule rule : rules) {
        for (FieldParsedContent bibField : bibFields) {
          if (isBibFieldLinkable(bibField, linkingMatchSubfield)) {
            suggestAuthorityForBibField(bibField, marcAuthoritiesContent, rule);
          }
        }
      }
    }
  }

  private boolean isBibFieldLinkable(FieldParsedContent bibField, String linkingMatchSubfield) {
    var linkDetails = bibField.getLinkDetails();
    return containsSubfield(bibField, linkingMatchSubfield)
      && (isNull(linkDetails) || linkDetails.getStatus() != NEW);
  }

  private boolean containsSubfield(FieldParsedContent bibField, String linkingMatchSubfield) {
    return isNotEmpty(bibField.getSubfields().get(linkingMatchSubfield));
  }

  private void suggestAuthorityForBibField(FieldParsedContent bibField,
                                           List<AuthorityParsedContent> marcAuthoritiesContent,
                                           InstanceAuthorityLinkingRule rule) {
    if (isFalse(rule.getAutoLinkingEnabled())) {
      var errorDetails = getErrorDetails(DISABLED_AUTO_LINKING);
      bibField.setLinkDetails(errorDetails);
      log.info("Field {}: auto linking feature is disabled", rule.getBibField());
      return;
    }

    var suitableAuthorities = filterSuitableAuthorities(bibField, marcAuthoritiesContent, rule);
    if (suitableAuthorities.isEmpty()) {
      var errorDetails = getErrorDetails(NO_SUGGESTIONS);
      bibField.setLinkDetails(errorDetails);
      log.info("Field {}: No authorities to suggest", rule.getBibField());
    } else if (suitableAuthorities.size() > 1) {
      var errorDetails = getErrorDetails(MORE_THAN_ONE_SUGGESTIONS);
      bibField.setLinkDetails(errorDetails);
      log.info("Field {}: More than one authority to suggest", rule.getBibField());
    } else {
      var authority = suitableAuthorities.get(0);
      var linkDetails = getLinkDetails(bibField, authority, rule);
      actualizeBibSubfields(bibField, authority, rule);
      bibField.setLinkDetails(linkDetails);
      log.info("Field {}: Authority {} was suggested", rule.getBibField(), authority.getId());
    }
  }

  private LinkDetails getLinkDetails(FieldParsedContent bibField,
                                     AuthorityParsedContent authority,
                                     InstanceAuthorityLinkingRule rule) {
    var linkDetails = bibField.getLinkDetails();
    if (isNull(linkDetails)) {
      linkDetails = new LinkDetails();
      linkDetails.setStatus(NEW);
    }
    linkDetails.setLinkingRuleId(rule.getId());
    linkDetails.setAuthorityId(authority.getId());
    linkDetails.setAuthorityNaturalId(authority.getNaturalId());
    return linkDetails;
  }

  private LinkDetails getErrorDetails(ErrorCode errorCode) {
    return new LinkDetails().status(ERROR).errorCause(errorCode.getErrorCode());
  }

  private void actualizeBibSubfields(FieldParsedContent bibField,
                                     AuthorityParsedContent authority,
                                     InstanceAuthorityLinkingRule rule) {
    var bibSubfields = bibField.getSubfields();
    var optionalField = authority.getFields().stream()
      .filter(fieldParsedContent -> fieldParsedContent.getTag().equals(rule.getAuthorityField()))
      .findFirst();
    if (optionalField.isEmpty()) {
      return;
    }

    var zeroValue = getSubfield0Value(sourceFilesService.fetchAuthoritySources(), authority.getNaturalId());
    bibSubfields.putAll(optionalField.get().getSubfields());
    bibSubfields.put("0", List.of(zeroValue));
    bibSubfields.put("9", List.of(authority.getId().toString()));

    modifySubfields(bibSubfields, rule);
  }

  private void modifySubfields(Map<String, List<String>> bibSubfields, InstanceAuthorityLinkingRule rule) {
    var modifications = rule.getSubfieldModifications();
    if (isNotEmpty(modifications)) {
      modifications.forEach(modification -> {
        var modifiedSubfield = bibSubfields.remove(modification.getSource());
        bibSubfields.put(modification.getTarget(), modifiedSubfield);
      });
    }
  }

  private List<AuthorityParsedContent> filterSuitableAuthorities(FieldParsedContent bibField,
                                                                 List<AuthorityParsedContent> marcAuthoritiesContent,
                                                                 InstanceAuthorityLinkingRule rule) {
    return marcAuthoritiesContent.stream()
      .filter(authorityContent -> validateSubfields(authorityContent, bibField))
      .filter(authorityContent -> authorityRuleValidationService.validateAuthorityFields(authorityContent, rule))
      .toList();
  }

  private boolean validateSubfields(AuthorityParsedContent marcAuthorityContent, FieldParsedContent bibField) {
    return Optional.ofNullable(bibField.getSubfields().get("0"))
      .map(subfields -> subfields.stream()
        .filter(Objects::nonNull)
        .map(FieldUtils::trimSubfield0Value)
        .anyMatch(zeroValue -> zeroValue.equals(marcAuthorityContent.getNaturalId())))
      .orElse(false)
      || Optional.ofNullable(bibField.getSubfields().get("9"))
      .map(subfields -> subfields.stream()
        .filter(Objects::nonNull)
        .anyMatch(nineValue -> nineValue.equals(marcAuthorityContent.getId().toString())))
      .orElse(false);
  }
}
