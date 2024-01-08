package org.folio.entlinks.service.links;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_LINKING_RULES_CACHE;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.repository.LinkingRulesRepository;
import org.folio.entlinks.exception.LinkingRuleNotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@UnitTest
@EnableCaching
@ExtendWith(SpringExtension.class)
@Import(InstanceAuthorityLinkingRulesService.class)
@ImportAutoConfiguration(CacheAutoConfiguration.class)
class InstanceAuthorityLinkingRulesServiceTest {

  private @MockBean LinkingRulesRepository repository;

  private @Autowired InstanceAuthorityLinkingRulesService service;
  private @Autowired CacheManager cacheManager;

  @Test
  void getLinkingRules_positive() {
    var rule = InstanceAuthorityLinkingRule.builder()
      .id(1)
      .bibField("100")
      .authorityField("100")
      .authoritySubfields(new char[] {'a', 'b'})
      .subfieldsExistenceValidations(Map.of("a", true))
      .subfieldModifications(List.of(new SubfieldModification().target("a").source("b")))
      .build();

    when(repository.findAll(any(Sort.class))).thenReturn(List.of(rule));

    var actual = service.getLinkingRules();

    assertThat(actual)
      .containsExactlyInAnyOrder(rule);
  }

  @Test
  void getLinkingRulesByAuthorityField_positive() {
    var authorityField = "101";
    var rule = InstanceAuthorityLinkingRule.builder()
      .id(1)
      .bibField("100")
      .authorityField(authorityField)
      .authoritySubfields(new char[] {'a', 'b'})
      .subfieldsExistenceValidations(Map.of("a", true))
      .subfieldModifications(List.of(new SubfieldModification().target("a").source("b")))
      .build();

    when(repository.findByAuthorityField(authorityField)).thenReturn(List.of(rule));

    var actual = service.getLinkingRulesByAuthorityField(authorityField);

    assertThat(actual)
      .containsExactlyInAnyOrder(rule);

    assertThat(getCache().get(TENANT_ID + ":" + authorityField))
      .as("Rule cached")
      .extracting(Cache.ValueWrapper::get)
      .isEqualTo(actual);
  }

  @Test
  void getLinkingRule_positive() {
    var ruleId = 1;
    var rule = InstanceAuthorityLinkingRule.builder()
      .id(ruleId)
      .bibField("100")
      .authorityField("101")
      .authoritySubfields(new char[] {'a', 'b'})
      .subfieldsExistenceValidations(Map.of("a", true))
      .subfieldModifications(List.of(new SubfieldModification().target("a").source("b")))
      .build();

    when(repository.findById(ruleId)).thenReturn(Optional.of(rule));

    var actual = service.getLinkingRule(ruleId);

    assertThat(actual).isEqualTo(rule);
  }

  @Test
  void getLinkingRule_negative_notFound() {
    var ruleId = 1;

    when(repository.findById(ruleId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getLinkingRule(ruleId))
      .isInstanceOf(LinkingRuleNotFoundException.class)
      .hasMessage(String.format("Linking rule with ID [%s] was not found", ruleId));
  }

  @Test
  void patchLinkingRule_positive_onlyExpectedFieldAndCacheInvalidated() {
    var ruleId = 1;
    var existedRule = InstanceAuthorityLinkingRule.builder()
      .id(ruleId)
      .bibField("100")
      .authorityField("101")
      .authoritySubfields(new char[] {'a', 'b'})
      .subfieldsExistenceValidations(Map.of("a", true))
      .subfieldModifications(List.of(new SubfieldModification().target("a").source("b")))
      .autoLinkingEnabled(false)
      .build();

    // add some value into cache
    getCache().put(TENANT_ID, existedRule);

    when(repository.findById(ruleId)).thenReturn(Optional.of(existedRule));

    var linkingRulePatch = new InstanceAuthorityLinkingRule();
    linkingRulePatch.setBibField("123");
    linkingRulePatch.setAuthorityField("321");
    linkingRulePatch.setAuthoritySubfields(new char[0]);
    linkingRulePatch.setSubfieldModifications(Collections.emptyList());
    linkingRulePatch.setSubfieldsExistenceValidations(Collections.emptyMap());
    linkingRulePatch.setAutoLinkingEnabled(true);
    service.patchLinkingRule(ruleId, linkingRulePatch);

    var ruleUpdateCaptor = ArgumentCaptor.forClass(InstanceAuthorityLinkingRule.class);

    verify(repository).save(ruleUpdateCaptor.capture());

    assertThat(getCache().get(TENANT_ID)).as("Cache invalidated").isNull();
    assertThat(ruleUpdateCaptor.getValue())
      .extracting(InstanceAuthorityLinkingRule::getId,
        InstanceAuthorityLinkingRule::getBibField,
        InstanceAuthorityLinkingRule::getAuthorityField,
        InstanceAuthorityLinkingRule::getAuthoritySubfields,
        InstanceAuthorityLinkingRule::getSubfieldModifications,
        InstanceAuthorityLinkingRule::getSubfieldsExistenceValidations,
        InstanceAuthorityLinkingRule::getAutoLinkingEnabled)
      .containsExactly(existedRule.getId(),
        existedRule.getBibField(),
        existedRule.getAuthorityField(),
        existedRule.getAuthoritySubfields(),
        existedRule.getSubfieldModifications(),
        existedRule.getSubfieldsExistenceValidations(),
        linkingRulePatch.getAutoLinkingEnabled());
  }

  @Test
  void patchLinkingRule_negative_notFound() {
    var ruleId = 1;

    when(repository.findById(ruleId)).thenReturn(Optional.empty());

    var linkingRulePatch = new InstanceAuthorityLinkingRule();
    assertThatThrownBy(() -> service.patchLinkingRule(ruleId, linkingRulePatch))
      .isInstanceOf(LinkingRuleNotFoundException.class)
      .hasMessage(String.format("Linking rule with ID [%s] was not found", ruleId));
  }

  private Cache getCache() {
    return cacheManager.getCache(AUTHORITY_LINKING_RULES_CACHE);
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    public FolioExecutionContext folioExecutionContext() {
      return new FolioExecutionContext() {
        @Override
        public String getTenantId() {
          return TENANT_ID;
        }
      };
    }
  }
}
