package org.folio.entlinks.controller.delegate.suggestion;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.controller.converter.SourceContentMapper;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.repository.AuthorityDataRepository;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.internal.SearchService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.LinksSuggestionService;
import org.folio.entlinks.utils.FieldUtils;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class LinksSuggestionsByAuthorityNaturalId extends LinksSuggestionsServiceDelegateBase<String> {
  private final AuthorityDataRepository dataRepository;
  private final SearchService searchService;

  public LinksSuggestionsByAuthorityNaturalId(InstanceAuthorityLinkingRulesService linkingRulesService,
                                              LinksSuggestionService suggestionService,
                                              AuthorityDataRepository dataRepository,
                                              SourceStorageClient sourceStorageClient,
                                              SourceContentMapper contentMapper,
                                              SearchService searchService) {
    super(linkingRulesService, suggestionService, dataRepository, sourceStorageClient, contentMapper);
    this.dataRepository = dataRepository;
    this.searchService = searchService;
  }

  @Override
  protected String getSearchSubfield() {
    return "0";
  }

  @Override
  protected Set<String> extractIds(FieldParsedContent field) {
    var naturalIds = new HashSet<String>();
    var zeroValues = field.getSubfields().get("0");
    if (isNotEmpty(zeroValues)) {
      naturalIds.addAll(zeroValues.stream()
        .map(FieldUtils::trimSubfield0Value)
        .collect(Collectors.toSet()));
    }
    if (nonNull(field.getLinkDetails()) && !isEmpty(field.getLinkDetails().getAuthorityNaturalId())) {
      naturalIds.add(field.getLinkDetails().getAuthorityNaturalId());
    }
    return naturalIds;
  }

  @Override
  protected List<AuthorityData> findExistingAuthorities(Set<String> ids) {
    return dataRepository.findByNaturalIds(ids);
  }

  @Override
  protected String extractId(AuthorityData authorityData) {
    return authorityData.getNaturalId();
  }

  @Override
  protected List<AuthorityData> searchAuthorities(Set<String> ids) {
    return searchService.searchAuthoritiesByNaturalIds(new ArrayList<>(ids));
  }
}
