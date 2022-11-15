package org.folio.entlinks.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.folio.entlinks.model.entity.LinkingRules;
import org.folio.qm.domain.dto.LinkingRuleDto;
import org.mapstruct.Mapper;
import org.springframework.boot.json.JsonParseException;

@Mapper(componentModel = "spring")
public interface LinkingRulesMapper {

  ObjectMapper MAPPER = new ObjectMapper();

  default List<LinkingRuleDto> convert(LinkingRules linkingRules) {
    try {
      var rules = MAPPER.readValue(linkingRules.getJsonb(), LinkingRuleDto[].class);
      return List.of(rules);
    } catch (JsonProcessingException e) {
      throw new JsonParseException(e);
    }
  }
}
