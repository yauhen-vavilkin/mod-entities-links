package org.folio.entlinks.service.messaging.authority;

import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.integration.internal.MappingRulesService;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorityMappingRulesProcessingService {

  private final MappingRulesService mappingRulesService;

  public String getTagByAuthorityChangeField(AuthorityChangeField authorityChangeField) {
    var mappingRelations = mappingRulesService.getFieldTargetsMappingRelations();
    return mappingRelations.entrySet().stream()
      .filter(mappingRelation -> mappingRelation.getValue().contains(authorityChangeField.getFieldName()))
      .findFirst()
      .map(Map.Entry::getKey)
      .orElse(null);
  }

  public Map<AuthorityChangeField, String> getFieldTagRelations() {
    Map<AuthorityChangeField, String> map = new EnumMap<>(AuthorityChangeField.class);
    for (AuthorityChangeField authorityChangeField : AuthorityChangeField.values()) {
      var fieldTagRelation = getTagByAuthorityChangeField(authorityChangeField);
      if (fieldTagRelation != null) {
        map.put(authorityChangeField, fieldTagRelation);
      }
    }
    return map;
  }
}
