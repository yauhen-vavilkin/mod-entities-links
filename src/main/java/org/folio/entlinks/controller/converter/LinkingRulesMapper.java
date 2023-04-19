package org.folio.entlinks.controller.converter;

import java.util.List;
import java.util.Map;
import org.folio.entlinks.domain.dto.LinkingRuleDto;
import org.folio.entlinks.domain.dto.LinkingRulePatchRequest;
import org.folio.entlinks.domain.dto.SubfieldValidation;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LinkingRulesMapper {

  List<LinkingRuleDto> convert(List<InstanceAuthorityLinkingRule> instanceAuthorityLinkingRule);

  @Mapping(target = "validation", expression = "java(convert(source.getSubfieldsExistenceValidations()))")
  LinkingRuleDto convert(InstanceAuthorityLinkingRule source);

  @Mapping(target = "subfieldsExistenceValidations", ignore = true)
  @Mapping(target = "subfieldModifications", ignore = true)
  @Mapping(target = "bibField", ignore = true)
  @Mapping(target = "authoritySubfields", ignore = true)
  @Mapping(target = "authorityField", ignore = true)
  InstanceAuthorityLinkingRule convert(LinkingRulePatchRequest patchRequest);

  default SubfieldValidation convert(Map<String, Boolean> existence) {
    if (existence == null || existence.isEmpty()) {
      return null;
    }
    return new SubfieldValidation().existence(List.of(existence));
  }
}
