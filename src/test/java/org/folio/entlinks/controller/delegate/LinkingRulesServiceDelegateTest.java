package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.folio.entlinks.controller.converter.LinkingRulesMapper;
import org.folio.entlinks.domain.dto.LinkingRuleDto;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.dto.SubfieldValidation;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.spring.test.type.UnitTest;
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
    var modification = new SubfieldModification().source("a").target("b");
    var existence = Map.of("a", true);

    var rule = new InstanceAuthorityLinkingRule();
    rule.setId(1);
    rule.setBibField("100");
    rule.setAuthorityField("100");
    rule.setAuthoritySubfields(new char[] {'a', 'b'});
    rule.setSubfieldModifications(List.of(modification));
    rule.setSubfieldsExistenceValidations(existence);

    var ruleDto = new LinkingRuleDto()
      .bibField("100")
      .authorityField("100")
      .authoritySubfields(List.of("a", "b"))
      .addSubfieldModificationsItem(modification)
      .validation(new SubfieldValidation().addExistenceItem(existence));

    var linkingRules = List.of(rule);
    when(linkingRulesService.getLinkingRules()).thenReturn(linkingRules);
    when(mapper.convert(linkingRules)).thenReturn(List.of(ruleDto));

    var actual = delegate.getLinkingRules();

    assertThat(actual)
      .hasSize(1)
      .containsExactlyInAnyOrder(ruleDto);
  }
}
