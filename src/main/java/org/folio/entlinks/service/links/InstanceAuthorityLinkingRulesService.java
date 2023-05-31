package org.folio.entlinks.service.links;

import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_LINKING_RULES_CACHE;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.repository.LinkingRulesRepository;
import org.folio.entlinks.exception.LinkingRuleNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceAuthorityLinkingRulesService {

  private static final String MIN_AVAILABLE_AUTHORITY_FIELD = "100";
  private static final String MAX_AVAILABLE_AUTHORITY_FIELD = "150";
  private final LinkingRulesRepository repository;

  @Cacheable(cacheNames = AUTHORITY_LINKING_RULES_CACHE,
    key = "@folioExecutionContext.tenantId",
    unless = "#result.isEmpty()")
  public List<InstanceAuthorityLinkingRule> getLinkingRules() {
    log.info("Loading linking rules");
    return repository.findAll(Sort.by("id").ascending());
  }

  @Cacheable(cacheNames = AUTHORITY_LINKING_RULES_CACHE,
    key = "@folioExecutionContext.tenantId + ':' + #authorityField", unless = "#result.isEmpty()")
  public List<InstanceAuthorityLinkingRule> getLinkingRulesByAuthorityField(String authorityField) {
    log.info("Loading linking rules for [authorityField: {}]", authorityField);
    return repository.findByAuthorityField(authorityField);
  }

  public InstanceAuthorityLinkingRule getLinkingRule(Integer ruleId) {
    log.info("Loading linking rule [ruleId: {}]", ruleId);
    return repository.findById(ruleId)
      .orElseThrow(() -> new LinkingRuleNotFoundException(ruleId));
  }

  @Transactional
  @CacheEvict(cacheNames = AUTHORITY_LINKING_RULES_CACHE, allEntries = true)
  public void patchLinkingRule(Integer ruleId, InstanceAuthorityLinkingRule linkingRulePatch) {
    log.info("Patch linking rule [ruleId: {}, change: {}]", ruleId, linkingRulePatch);
    var existedLinkingRule = repository.findById(ruleId)
      .orElseThrow(() -> new LinkingRuleNotFoundException(ruleId));
    // only autoLinkingEnabled flag is allowed to be updated right now
    if (linkingRulePatch.getAutoLinkingEnabled() != null) {
      existedLinkingRule.setAutoLinkingEnabled(linkingRulePatch.getAutoLinkingEnabled());
    }
    repository.save(existedLinkingRule);
  }

  public String getMinAuthorityField() {
    return MIN_AVAILABLE_AUTHORITY_FIELD;
  }

  public String getMaxAuthorityField() {
    return MAX_AVAILABLE_AUTHORITY_FIELD;
  }
}
