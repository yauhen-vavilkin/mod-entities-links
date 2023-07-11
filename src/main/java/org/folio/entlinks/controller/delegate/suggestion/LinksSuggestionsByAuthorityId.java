package org.folio.entlinks.controller.delegate.suggestion;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
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
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class LinksSuggestionsByAuthorityId extends LinksSuggestionsServiceDelegateBase<UUID> {

  private static final Pattern UUID_REGEX =
    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  private final AuthorityDataRepository dataRepository;
  private final SearchService searchService;

  public LinksSuggestionsByAuthorityId(InstanceAuthorityLinkingRulesService linkingRulesService,
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
    return "9";
  }

  @Override
  protected Set<UUID> extractIds(FieldParsedContent field) {
    var ids = new HashSet<UUID>();
    var subfieldValues = field.getSubfields().get("9");
    if (isNotEmpty(subfieldValues)) {
      ids.addAll(subfieldValues.stream()
        .filter(id -> UUID_REGEX.matcher(id).matches())
        .map(UUID::fromString)
        .collect(Collectors.toSet()));
    }
    if (nonNull(field.getLinkDetails()) && nonNull(field.getLinkDetails().getAuthorityId())) {
      ids.add(field.getLinkDetails().getAuthorityId());
    }
    return ids;
  }

  @Override
  protected List<AuthorityData> findExistingAuthorities(Set<UUID> ids) {
    return dataRepository.findAllById(ids);
  }

  @Override
  protected UUID extractId(AuthorityData authorityData) {
    return authorityData.getId();
  }

  @Override
  protected List<AuthorityData> searchAuthorities(Set<UUID> ids) {
    return searchService.searchAuthoritiesByIds(new ArrayList<>(ids));
  }
}
