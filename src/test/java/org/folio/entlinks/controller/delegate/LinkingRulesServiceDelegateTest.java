package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.folio.entlinks.controller.converter.LinkingRulesMapper;
import org.folio.entlinks.domain.dto.LinkingRuleDto;
import org.folio.entlinks.domain.dto.LinkingRulePatchRequest;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.dto.SubfieldValidation;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.spring.testing.type.UnitTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinkingRulesServiceDelegateTest {

  private @Mock InstanceAuthorityLinkingRulesService linkingRulesService;
  private @Mock LinkingRulesMapper mapper;

  private @InjectMocks LinkingRulesServiceDelegate delegate;

  @Test
  void getLinkingRules() {
    InstanceAuthorityLinkingRule rule = createInstanceAuthorityLinkingRule();

    LinkingRuleDto ruleDto = createLinkingRuleDto();

    var linkingRules = List.of(rule);
    when(linkingRulesService.getLinkingRules()).thenReturn(linkingRules);
    when(mapper.convert(linkingRules)).thenReturn(List.of(ruleDto));

    var actual = delegate.getLinkingRules();

    assertThat(actual)
        .hasSize(1)
        .containsExactlyInAnyOrder(ruleDto);
  }

  @Test
  void testGetLinkingRuleById() {
    Integer ruleId = 1;
    var linkingRule = createInstanceAuthorityLinkingRule();
    var ruleDto = createLinkingRuleDto();

    when(linkingRulesService.getLinkingRule(ruleId)).thenReturn(linkingRule);
    when(mapper.convert(linkingRule)).thenReturn(ruleDto);

    LinkingRuleDto result = delegate.getLinkingRuleById(ruleId);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(linkingRule.getId());
    assertThat(result.getBibField()).isEqualTo(linkingRule.getBibField());
    assertThat(result.getAuthorityField()).isEqualTo(linkingRule.getAuthorityField());
    assertThat(result.getAuthoritySubfields()).isEqualTo(List.of("a", "b"));
    assertThat(result.getSubfieldModifications()).hasSize(1);
    assertThat(result.getSubfieldModifications().get(0).getSource()).isEqualTo("a");
    assertThat(result.getSubfieldModifications().get(0).getTarget()).isEqualTo("b");
    assertThat(result.getValidation().getExistence()).hasSize(1);
  }

  @Test
  void testPatchLinkingRuleById() {
    Integer ruleId = 1;
    LinkingRulePatchRequest patchRequest = new LinkingRulePatchRequest();
    patchRequest.setId(ruleId);
    InstanceAuthorityLinkingRule linkingRule = createInstanceAuthorityLinkingRule();

    when(mapper.convert(patchRequest)).thenReturn(linkingRule);

    delegate.patchLinkingRuleById(ruleId, patchRequest);

    verify(linkingRulesService, times(1)).patchLinkingRule(ruleId, linkingRule);
  }

  @Test
  void testPatchLinkingRuleById_ExceptionCase() {
    Integer ruleId = 2;
    LinkingRulePatchRequest patchRequest = new LinkingRulePatchRequest();
    InstanceAuthorityLinkingRule linkingRule = createInstanceAuthorityLinkingRule();

    when(mapper.convert(patchRequest)).thenReturn(linkingRule);

    var exception = assertThrows(RequestBodyValidationException.class, () -> {
      delegate.patchLinkingRuleById(ruleId, patchRequest);
    });

    assertThat("Request should have id = " + ruleId).isEqualTo(exception.getMessage());

    verify(linkingRulesService, never()).patchLinkingRule(any(), any());
  }

  @NotNull
  private static InstanceAuthorityLinkingRule createInstanceAuthorityLinkingRule() {
    var modification = new SubfieldModification().source("a").target("b");
    var existence = Map.of("a", true);
    var rule = new InstanceAuthorityLinkingRule();
    rule.setId(1);
    rule.setBibField("100");
    rule.setAuthorityField("100");
    rule.setAuthoritySubfields(new char[]{'a', 'b'});
    rule.setSubfieldModifications(List.of(modification));
    rule.setSubfieldsExistenceValidations(existence);
    return rule;
  }

  @NotNull
  private static LinkingRuleDto createLinkingRuleDto() {
    var modification = new SubfieldModification().source("a").target("b");
    var existence = Map.of("a", true);
    return new LinkingRuleDto()
        .id(1)
        .bibField("100")
        .authorityField("100")
        .authoritySubfields(List.of("a", "b"))
        .addSubfieldModificationsItem(modification)
        .validation(new SubfieldValidation().addExistenceItem(existence));
  }
}
