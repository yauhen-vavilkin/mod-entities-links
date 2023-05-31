package org.folio.entlinks.controller.delegate;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.removeAll;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.client.SearchClient;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.controller.converter.DataMapper;
import org.folio.entlinks.controller.converter.SourceContentMapper;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.entlinks.domain.dto.StrippedParsedRecordCollection;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.repository.AuthorityDataRepository;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.LinksSuggestionService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class LinksSuggestionsServiceDelegate {

  private final InstanceAuthorityLinkingRulesService linkingRulesService;
  private final LinksSuggestionService suggestionService;
  private final AuthorityDataRepository dataRepository;
  private final SourceStorageClient sourceStorageClient;
  private final SourceContentMapper contentMapper;
  private final SearchClient searchClient;
  private final DataMapper dataMapper;

  public ParsedRecordContentCollection suggestLinksForMarcRecords(ParsedRecordContentCollection contentCollection) {
    var rules = rulesToBibFieldMap(linkingRulesService.getLinkingRules());
    var marcBibsContent = contentMapper.convertToParsedContent(contentCollection);
    var naturalIds = extractNaturalIdsOfLinkableFields(marcBibsContent, rules);

    var authorities = findAuthoritiesByNaturalIds(naturalIds);
    var marcAuthorities = fetchAuthorityParsedRecords(authorities);
    var marcAuthoritiesContent = contentMapper.convertToAuthorityParsedContent(marcAuthorities, authorities);

    suggestionService.fillLinkDetailsWithSuggestedAuthorities(marcBibsContent, marcAuthoritiesContent, rules);

    return contentMapper.convertToParsedContentCollection(marcBibsContent);
  }

  private List<AuthorityData> findAuthoritiesByNaturalIds(Set<String> naturalIds) {
    var authorityData = dataRepository.findByNaturalIds(naturalIds);
    if (authorityData.size() != naturalIds.size()) {
      var existNaturalIds = authorityData.stream()
        .map(AuthorityData::getNaturalId)
        .collect(Collectors.toSet());

      var naturalIdsToSearch = new HashSet<>(removeAll(naturalIds, existNaturalIds));
      authorityData.addAll(searchAndSaveAuthorities(naturalIdsToSearch));
    }
    return authorityData;
  }

  private StrippedParsedRecordCollection fetchAuthorityParsedRecords(List<AuthorityData> authorityData) {
    if (isNotEmpty(authorityData)) {
      var ids = authorityData.stream().map(AuthorityData::getId).collect(Collectors.toSet());
      var authorityFetchRequest = sourceStorageClient.buildBatchFetchRequestForAuthority(ids,
        linkingRulesService.getMinAuthorityField(),
        linkingRulesService.getMaxAuthorityField());

      return sourceStorageClient.fetchParsedRecordsInBatch(authorityFetchRequest);
    }
    return new StrippedParsedRecordCollection(Collections.emptyList(), 0);
  }

  private List<AuthorityData> searchAndSaveAuthorities(Set<String> naturalIds) {
    var query = searchClient.buildNaturalIdsQuery(naturalIds);

    var authorityData = searchClient.searchAuthorities(query, false)
      .getAuthorities().stream()
      .map(dataMapper::convertToData)
      .toList();

    return dataRepository.saveAll(authorityData);
  }

  private Set<String> extractNaturalIdsOfLinkableFields(List<SourceParsedContent> contentCollection,
                                                        Map<String, List<InstanceAuthorityLinkingRule>> rules) {
    return contentCollection.stream()
      .flatMap(bibRecord -> bibRecord.getFields().entrySet().stream())
      .filter(field -> isAutoLinkingEnabled(rules.get(field.getKey())))
      .map(field -> extractNaturalIdFrom0Subfield(field.getValue()))
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  private String extractNaturalIdFrom0Subfield(FieldParsedContent fieldContent) {
    var value0 = fieldContent.getSubfields().get("0");
    if (nonNull(value0)) {
      var slashIndex = value0.lastIndexOf('/');
      if (slashIndex != -1) {
        return value0.substring(slashIndex + 1);
      }
      return value0;
    } else if (nonNull(fieldContent.getLinkDetails())) {
      return fieldContent.getLinkDetails().getNaturalId();
    }
    return null;
  }

  private boolean isAutoLinkingEnabled(List<InstanceAuthorityLinkingRule> rules) {
    if (nonNull(rules)) {
      return rules.stream().anyMatch(InstanceAuthorityLinkingRule::getAutoLinkingEnabled);
    }
    return false;
  }

  private Map<String, List<InstanceAuthorityLinkingRule>> rulesToBibFieldMap(List<InstanceAuthorityLinkingRule> rules) {
    return rules.stream().collect(groupingBy(InstanceAuthorityLinkingRule::getBibField));
  }
}
