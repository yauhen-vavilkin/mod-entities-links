package org.folio.entlinks.controller.delegate.suggestion;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
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
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class LinksSuggestionsByAuthorityId extends LinksSuggestionsServiceDelegateBase<UUID> {

  private static final Pattern UUID_REGEX =
    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  private static final String ID_SUBFIELD = "9";
  private final AuthorityRepository authorityRepository;

  public LinksSuggestionsByAuthorityId(InstanceAuthorityLinkingRulesService linkingRulesService,
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
    return ID_SUBFIELD;
  }

  @Override
  protected Set<UUID> extractIds(FieldParsedContent field) {
    var ids = new HashSet<UUID>();
    var subfieldValues = field.getSubfields().get(ID_SUBFIELD);
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
  protected List<Authority> findExistingAuthorities(Set<UUID> ids) {
    return authorityRepository.findAllByIdInAndDeletedFalse(ids);
  }

  @Override
  protected UUID extractId(Authority authority) {
    return authority.getId();
  }
}
