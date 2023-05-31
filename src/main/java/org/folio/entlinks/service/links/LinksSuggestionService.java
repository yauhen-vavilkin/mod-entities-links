package org.folio.entlinks.service.links;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.entlinks.domain.dto.LinkStatus.ACTUAL;
import static org.folio.entlinks.domain.dto.LinkStatus.ERROR;
import static org.folio.entlinks.domain.dto.LinkStatus.NEW;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.integration.dto.AuthorityParsedContent;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.folio.entlinks.integration.internal.AuthoritySourceFilesService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class LinksSuggestionService {
  private static final String NO_SUGGESTIONS_ERROR_CODE = "101";
  private static final String MORE_THEN_ONE_SUGGESTIONS_ERROR_CODE = "102";
  private final AuthoritySourceFilesService sourceFilesService;

  /**
   * Validate bib-authority fields by linking rules and fill bib fields with suggested links.
   *
   * @param marcBibsContent        list of bib records {@link SourceParsedContent}
   * @param marcAuthoritiesContent list of authorities {@link AuthorityParsedContent} that can be suggested as link
   * @param rules                  linking rules
   *                               <p>Key - bib tag, Value - list of {@link InstanceAuthorityLinkingRule}</p>
   */
  public void fillLinkDetailsWithSuggestedAuthorities(List<SourceParsedContent> marcBibsContent,
                                                      List<AuthorityParsedContent> marcAuthoritiesContent,
                                                      Map<String, List<InstanceAuthorityLinkingRule>> rules) {
    if (isNotEmpty(marcAuthoritiesContent)) {
      marcBibsContent.stream()
        .flatMap(bibContent -> bibContent.getFields().entrySet().stream())
        .forEach(bibField -> suggestAuthorityForBibField(
          bibField.getValue(),
          marcAuthoritiesContent,
          rules.get(bibField.getKey())));
    }
  }

  private void suggestAuthorityForBibField(FieldParsedContent bibField,
                                           List<AuthorityParsedContent> marcAuthoritiesContent,
                                           List<InstanceAuthorityLinkingRule> rules) {
    for (var rule : rules) {
      if (isTrue(rule.getAutoLinkingEnabled())) {
        var suitableAuthorities = marcAuthoritiesContent.stream()
          .filter(authorityContent -> validateAuthorityFields(authorityContent, rule))
          .toList();

        if (suitableAuthorities.isEmpty()) {
          var errorDetails = getErrorDetails(NO_SUGGESTIONS_ERROR_CODE);
          bibField.setLinkDetails(errorDetails);
        } else if (suitableAuthorities.size() > 1) {
          var errorDetails = getErrorDetails(MORE_THEN_ONE_SUGGESTIONS_ERROR_CODE);
          bibField.setLinkDetails(errorDetails);
        } else {
          var authority = suitableAuthorities.get(0);
          var linkDetails = getLinkDetails(bibField, authority, rule);
          actualizeBibSubfields(bibField, authority, rule);
          bibField.setLinkDetails(linkDetails);
          break;
        }
      }
    }
  }

  private LinkDetails getLinkDetails(FieldParsedContent bibField,
                                     AuthorityParsedContent authority,
                                     InstanceAuthorityLinkingRule rule) {
    var linkDetails = bibField.getLinkDetails();

    if (nonNull(linkDetails)) {
      linkDetails.setLinksStatus(ACTUAL);
    } else {
      linkDetails = new LinkDetails();
      linkDetails.setLinksStatus(NEW);
    }
    linkDetails.setRuleId(rule.getId());
    linkDetails.setAuthorityId(authority.getId());
    linkDetails.setNaturalId(authority.getNaturalId());
    return linkDetails;
  }

  private LinkDetails getErrorDetails(String errorCode) {
    return new LinkDetails().linksStatus(ERROR).errorStatusCode(errorCode);
  }

  private void actualizeBibSubfields(FieldParsedContent bibField,
                                     AuthorityParsedContent authority,
                                     InstanceAuthorityLinkingRule rule) {
    var bibSubfields = bibField.getSubfields();
    var authoritySubfields = authority.getFields()
      .get(rule.getAuthorityField())
      .getSubfields();

    bibSubfields.putAll(authoritySubfields);
    bibSubfields.put("0", getSubfield0Value(authority.getNaturalId()));
    bibSubfields.put("9", authority.getId().toString());

    var modifications = rule.getSubfieldModifications();
    if (nonNull(modifications)) {
      modifications.forEach(modification -> {
        var modifiedSubfield = bibSubfields.remove(modification.getSource());
        bibSubfields.put(modification.getTarget(), modifiedSubfield);
      });
    }
  }

  private boolean validateAuthorityFields(AuthorityParsedContent authorityContent, InstanceAuthorityLinkingRule rule) {
    var authorityField = authorityContent.getFields().get(rule.getAuthorityField());

    if (nonNull(authorityField)) {
      return validateAuthoritySubfields(authorityField, rule);
    }
    return false;
  }

  private boolean validateAuthoritySubfields(FieldParsedContent authorityField,
                                             InstanceAuthorityLinkingRule rule) {
    var existValidation = rule.getSubfieldsExistenceValidations();
    if (!existValidation.isEmpty()) {
      var authoritySubfields = authorityField.getSubfields();

      for (var subfieldExistence : existValidation.entrySet()) {
        var contains = authoritySubfields.containsKey(subfieldExistence.getKey());
        if (contains != subfieldExistence.getValue()) {
          return false;
        }
      }
    }
    return true;
  }

  private String getSubfield0Value(String naturalId) {
    var subfield0Value = "";
    if (nonNull(naturalId)) {
      var files = sourceFilesService.fetchAuthoritySources();
      var sourceFile = sourceFilesService.findAuthoritySourceFileByNaturalId(files, naturalId);
      if (sourceFile != null) {
        subfield0Value = StringUtils.appendIfMissing(sourceFile.baseUrl(), "/");
      }
    }
    return subfield0Value + naturalId;
  }
}
