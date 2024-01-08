package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.TEST_PROPERTY_VALUE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.MapAssert;
import org.folio.entlinks.domain.dto.LinkingRuleDto;
import org.folio.entlinks.domain.dto.LinkingRulePatchRequest;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.dto.SubfieldValidation;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class LinkingRulesMapperTest {

  private final LinkingRulesMapperImpl mapper = new LinkingRulesMapperImpl();

  @Test
  void testConvert_ListOfInstanceAuthorityLinkingRule() {
    InstanceAuthorityLinkingRule rule = createSubfieldModification();
    var sourceList = List.of(rule);

    List<LinkingRuleDto> dtoList = mapper.convert(sourceList);

    ListAssert.assertThatList(sourceList).hasSameSizeAs(dtoList);
    LinkingRuleDto linkingRuleDto = dtoList.get(0);
    assertThat(linkingRuleDto.getId()).isEqualTo(rule.getId());
    assertThat(linkingRuleDto.getBibField()).isEqualTo(rule.getBibField());
    assertThat(linkingRuleDto.getAuthorityField()).isEqualTo(rule.getAuthorityField());
    assertThat(linkingRuleDto.getAuthoritySubfields().get(0).charAt(0)).isEqualTo(rule.getAuthoritySubfields()[0]);
    SubfieldModification modification = linkingRuleDto.getSubfieldModifications().get(0);
    assertThat(modification.getTarget()).isEqualTo(rule.getSubfieldModifications().get(0).getTarget());
    assertThat(modification.getSource()).isEqualTo(rule.getSubfieldModifications().get(0).getSource());
    assertThat(linkingRuleDto.getAutoLinkingEnabled()).isEqualTo(rule.getAutoLinkingEnabled());
  }

  @Test
  void testConvert_InstanceAuthorityLinkingRule() {
    InstanceAuthorityLinkingRule rule = createSubfieldModification();

    LinkingRuleDto dto = mapper.convert(rule);

    assertThat(dto.getId()).isEqualTo(rule.getId());
    assertThat(dto.getBibField()).isEqualTo(rule.getBibField());
    assertThat(dto.getAuthorityField()).isEqualTo(rule.getAuthorityField());
    assertThat(dto.getAuthoritySubfields().get(0).charAt(0)).isEqualTo(rule.getAuthoritySubfields()[0]);
    SubfieldModification modification = dto.getSubfieldModifications().get(0);
    assertThat(modification.getTarget()).isEqualTo(rule.getSubfieldModifications().get(0).getTarget());
    assertThat(modification.getSource()).isEqualTo(rule.getSubfieldModifications().get(0).getSource());
    assertThat(dto.getAutoLinkingEnabled()).isEqualTo(rule.getAutoLinkingEnabled());
  }

  @Test
  void testConvert_LinkingRulePatchRequest() {
    var patchRequest = new LinkingRulePatchRequest();
    patchRequest.setAutoLinkingEnabled(true);
    patchRequest.setId(1);

    InstanceAuthorityLinkingRule rule = mapper.convert(patchRequest);

    assertThat(rule.getAutoLinkingEnabled()).isEqualTo(patchRequest.getAutoLinkingEnabled());
    assertThat(rule.getId()).isEqualTo(patchRequest.getId());
  }

  @Test
  void testConvert_MapToSubfieldValidation() {
    Map<String, Boolean> existenceMap = new HashMap<>();
    String existence = "existence";
    existenceMap.put(existence, true);

    SubfieldValidation validation = mapper.convert(existenceMap);

    var existenceValue = existenceMap.get(existence);
    MapAssert.assertThatMap(validation.getExistence().get(0)).containsEntry(existence, existenceValue);
  }

  private InstanceAuthorityLinkingRule createSubfieldModification() {
    var subfieldModification = new SubfieldModification();
    subfieldModification.setSource("src");
    subfieldModification.setTarget("tgt");

    var rule = new InstanceAuthorityLinkingRule();
    rule.setBibField(TEST_PROPERTY_VALUE);
    rule.setAuthorityField(TEST_PROPERTY_VALUE);
    rule.setAuthoritySubfields(new char[]{'a', 'b'});
    rule.setAutoLinkingEnabled(true);
    rule.setSubfieldModifications(List.of(subfieldModification));
    return rule;
  }
}
