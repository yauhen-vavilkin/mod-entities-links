package org.folio.entlinks.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.entlinks.LinkingPairType;
import org.folio.entlinks.model.entity.LinkingRules;
import org.folio.qm.domain.dto.LinkingRuleDto;
import org.mapstruct.Mapper;
import org.springframework.boot.json.JsonParseException;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LinkingRulesMapper {

  ObjectMapper mapper = new ObjectMapper();

  default List<LinkingRuleDto> convert(LinkingRules linkingRules) {
    try {
      var rules = mapper.readValue(linkingRules.getData(), LinkingRuleDto[].class);
      return List.of(rules);
    } catch (JsonProcessingException e) {
      throw new JsonParseException(e);
    }
  }

  /**
   * Validate json string and convert it to JPA LinkingRules entity
   *
   * @param linkingPairType - pair type of linking rules
   * @param jsonRules - json string to convert
   */
  default LinkingRules convert(LinkingPairType linkingPairType, String jsonRules) {
    try {
      mapper.readTree(jsonRules);
      return LinkingRules.builder()
          .linkingPairType(linkingPairType.name())
          .data(jsonRules)
          .build();
    } catch (JsonProcessingException e) {
      throw new JsonParseException(e);
    }
  }
}
