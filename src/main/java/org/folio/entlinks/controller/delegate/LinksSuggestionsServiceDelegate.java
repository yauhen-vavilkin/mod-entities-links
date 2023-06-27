package org.folio.entlinks.controller.delegate;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.removeAll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.controller.converter.SourceContentMapper;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.entlinks.domain.dto.StrippedParsedRecordCollection;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.repository.AuthorityDataRepository;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.folio.entlinks.integration.internal.SearchService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.LinksSuggestionService;
import org.folio.entlinks.utils.FieldUtils;
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
  private final SearchService searchService;

  public ParsedRecordContentCollection suggestLinksForMarcRecords(ParsedRecordContentCollection contentCollection) {
    log.info("Links suggestion started for {} bibs", contentCollection.getRecords().size());
    var rules = rulesToBibFieldMap(linkingRulesService.getLinkingRules());
    var marcBibsContent = contentMapper.convertToParsedContent(contentCollection);

    var naturalIds = extractNaturalIdsOfLinkableFields(marcBibsContent, rules);
    log.info("{} natural ids was extracted", naturalIds.size());

    var authorities = findAuthoritiesByNaturalIds(naturalIds);
    log.info("{} authorities to suggest found", authorities.size());

    if (isNotEmpty(authorities)) {
      var marcAuthorities = fetchAuthorityParsedRecords(authorities);
      var marcAuthoritiesContent = contentMapper.convertToAuthorityParsedContent(marcAuthorities, authorities);
      suggestionService.fillLinkDetailsWithSuggestedAuthorities(marcBibsContent, marcAuthoritiesContent, rules);
    } else {
      suggestionService.fillErrorDetailsWithNoSuggestions(marcBibsContent);
    }

    return contentMapper.convertToParsedContentCollection(marcBibsContent);
  }

  private List<AuthorityData> findAuthoritiesByNaturalIds(Set<String> naturalIds) {
    var authorityData = dataRepository.findByNaturalIds(naturalIds);
    var existNaturalIds = authorityData.stream()
      .map(AuthorityData::getNaturalId)
      .collect(Collectors.toSet());
    log.info("{} authority data found by natural ids", authorityData.size());

    if (!existNaturalIds.containsAll(naturalIds)) {
      var naturalIdsToSearch = new HashSet<>(removeAll(naturalIds, existNaturalIds));
      var authoritiesFromSearch = searchAndSaveAuthorities(naturalIdsToSearch);
      log.info("{} authority data was saved", authoritiesFromSearch.size());

      authorityData.addAll(authoritiesFromSearch);
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
    var authorityData = searchService.searchAuthoritiesByNaturalIds(new ArrayList<>(naturalIds));
    return dataRepository.saveAll(authorityData);
  }

  private Set<String> extractNaturalIdsOfLinkableFields(List<SourceParsedContent> contentCollection,
                                                        Map<String, List<InstanceAuthorityLinkingRule>> rules) {
    return contentCollection.stream()
      .flatMap(bibRecord -> bibRecord.getFields().stream())
      .filter(field -> isAutoLinkingEnabled(rules.get(field.getTag())))
      .map(this::extractNaturalIds)
      .filter(CollectionUtils::isNotEmpty)
      .flatMap(Set::stream)
      .collect(Collectors.toSet());
  }

  private Set<String> extractNaturalIds(FieldParsedContent field) {
    var naturalIds = new HashSet<String>();
    var zeroValues = field.getSubfields().get("0");
    if (isNotEmpty(zeroValues)) {
      naturalIds.addAll(zeroValues.stream()
        .map(FieldUtils::trimSubfield0Value)
        .collect(Collectors.toSet()));
    }
    if (nonNull(field.getLinkDetails())) {
      naturalIds.add(field.getLinkDetails().getAuthorityNaturalId());
    }
    return naturalIds;
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
