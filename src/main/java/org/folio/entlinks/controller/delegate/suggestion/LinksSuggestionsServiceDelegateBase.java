package org.folio.entlinks.controller.delegate.suggestion;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.removeAll;

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
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.LinksSuggestionService;
import org.springframework.stereotype.Service;

/**
 * Base class for link suggestions delegates.
 * Inheritors are supposed to define authority field to extract from incoming data
 * and to search authorities by.
 * T generic is intended to define authority field data type.
 * */
@Log4j2
@Service
@RequiredArgsConstructor
public abstract class LinksSuggestionsServiceDelegateBase<T> implements LinksSuggestionServiceDelegate {

  private final InstanceAuthorityLinkingRulesService linkingRulesService;
  private final LinksSuggestionService suggestionService;
  private final AuthorityDataRepository dataRepository;
  private final SourceStorageClient sourceStorageClient;
  private final SourceContentMapper contentMapper;

  public ParsedRecordContentCollection suggestLinksForMarcRecords(ParsedRecordContentCollection contentCollection) {
    log.info("{}: Links suggestion started for {} bibs",
      this.getClass().getSimpleName(), contentCollection.getRecords().size());
    var rules = rulesToBibFieldMap(linkingRulesService.getLinkingRules());
    var marcBibsContent = contentMapper.convertToParsedContent(contentCollection);

    var authoritySearchIds = extractIdsOfLinkableFields(marcBibsContent, rules);
    log.info("{} authority search ids was extracted", authoritySearchIds.size());

    var authorities = findAuthorities(authoritySearchIds);
    log.info("{} authorities to suggest found", authorities.size());

    if (isNotEmpty(authorities)) {
      var marcAuthorities = fetchAuthorityParsedRecords(authorities);
      var marcAuthoritiesContent = contentMapper.convertToAuthorityParsedContent(marcAuthorities, authorities);
      suggestionService.fillLinkDetailsWithSuggestedAuthorities(marcBibsContent, marcAuthoritiesContent, rules,
        getSearchSubfield());
    } else {
      suggestionService.fillErrorDetailsWithNoSuggestions(marcBibsContent, getSearchSubfield());
    }

    return contentMapper.convertToParsedContentCollection(marcBibsContent);
  }

  protected abstract String getSearchSubfield();

  private List<AuthorityData> findAuthorities(Set<T> ids) {
    var authorityData = findExistingAuthorities(ids);
    var existIds = authorityData.stream()
      .map(this::extractId)
      .collect(Collectors.toSet());
    log.info("{} authority data found by ids", authorityData.size());

    if (!existIds.containsAll(ids)) {
      var idsToSearch = new HashSet<>(removeAll(ids, existIds));
      var authoritiesFromSearch = searchAndSaveAuthorities(idsToSearch);
      log.info("{} authority data was saved", authoritiesFromSearch.size());

      authorityData.addAll(authoritiesFromSearch);
    }
    return authorityData;
  }

  protected abstract List<AuthorityData> findExistingAuthorities(Set<T> ids);

  protected abstract T extractId(AuthorityData authorityData);

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

  private List<AuthorityData> searchAndSaveAuthorities(Set<T> ids) {
    var authorityData = searchAuthorities(ids);
    return dataRepository.saveAll(authorityData);
  }

  protected abstract List<AuthorityData> searchAuthorities(Set<T> ids);

  private Set<T> extractIdsOfLinkableFields(List<SourceParsedContent> contentCollection,
                                                 Map<String, List<InstanceAuthorityLinkingRule>> rules) {
    return contentCollection.stream()
      .flatMap(bibRecord -> bibRecord.getFields().stream())
      .filter(field -> isAutoLinkingEnabled(rules.get(field.getTag())))
      .map(this::extractIds)
      .filter(CollectionUtils::isNotEmpty)
      .flatMap(Set::stream)
      .collect(Collectors.toSet());
  }

  protected abstract Set<T> extractIds(FieldParsedContent field);

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
