package org.folio.entlinks.controller.delegate.suggestion;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.controller.converter.SourceContentMapper;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.service.consortium.ConsortiumTenantExecutor;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.LinksSuggestionService;
import org.folio.entlinks.utils.FieldUtils;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class LinksSuggestionsByAuthorityNaturalId extends LinksSuggestionsServiceDelegateBase<String> {

  private static final String NATURAL_ID_SUBFIELD = "0";

  private final AuthorityRepository authorityRepository;

  public LinksSuggestionsByAuthorityNaturalId(InstanceAuthorityLinkingRulesService linkingRulesService,
                                              LinksSuggestionService suggestionService,
                                              AuthorityRepository repository,
                                              SourceStorageClient sourceStorageClient,
                                              SourceContentMapper contentMapper,
                                              ConsortiumTenantExecutor executor) {
    super(linkingRulesService, suggestionService, sourceStorageClient, contentMapper, executor);
    this.authorityRepository = repository;
  }

  @Override
  protected String getSearchSubfield() {
    return NATURAL_ID_SUBFIELD;
  }

  @Override
  protected Set<String> extractIds(FieldParsedContent field) {
    var naturalIds = new HashSet<String>();
    var zeroValues = field.getSubfields().get(NATURAL_ID_SUBFIELD);
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
  protected List<Authority> findExistingAuthorities(Set<String> ids) {
    return authorityRepository.findByNaturalIdInAndDeletedFalse(ids);
  }

  @Override
  protected String extractId(Authority authority) {
    return authority.getNaturalId();
  }
}
