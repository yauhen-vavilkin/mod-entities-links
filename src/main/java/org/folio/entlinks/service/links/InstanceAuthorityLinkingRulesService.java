package org.folio.entlinks.service.links;

import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_LINKING_RULES_CACHE;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.repository.LinkingRulesRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceAuthorityLinkingRulesService {

  private final LinkingRulesRepository repository;

  public List<InstanceAuthorityLinkingRule> getLinkingRules() {
    log.info("Loading linking rules");
    return repository.findAll();
  }

  @Cacheable(cacheNames = AUTHORITY_LINKING_RULES_CACHE,
             key = "@folioExecutionContext.tenantId + ':' + #authorityField", unless = "#result.isEmpty()")
  public List<InstanceAuthorityLinkingRule> getLinkingRulesByAuthorityField(String authorityField) {
    log.info("Loading linking rules for [authorityField: {}]", authorityField);
    return repository.findByAuthorityField(authorityField);
  }

}
