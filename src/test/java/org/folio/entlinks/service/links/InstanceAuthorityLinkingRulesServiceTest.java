package org.folio.entlinks.service.links;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.repository.LinkingRulesRepository;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceAuthorityLinkingRulesServiceTest {

  private @Mock LinkingRulesRepository repository;

  private @InjectMocks InstanceAuthorityLinkingRulesService service;

  @Test
  void getLinkingRules_positive() {
    var rule = InstanceAuthorityLinkingRule.builder()
      .id(1L)
      .bibField("100")
      .authorityField("100")
      .authoritySubfields(new char[] {'a', 'b'})
      .subfieldsExistenceValidations(Map.of("a", true))
      .subfieldModifications(List.of(new SubfieldModification().target("a").source("b")))
      .build();

    when(repository.findAll()).thenReturn(List.of(rule));

    var actual = service.getLinkingRules();

    assertThat(actual)
      .containsExactlyInAnyOrder(rule);
  }

  @Test
  void getLinkingRulesByAuthorityField_positive() {
    var authorityField = "101";
    var rule = InstanceAuthorityLinkingRule.builder()
      .id(1L)
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
  }
}
