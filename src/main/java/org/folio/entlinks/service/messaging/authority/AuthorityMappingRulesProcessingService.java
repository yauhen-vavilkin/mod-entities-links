package org.folio.entlinks.service.messaging.authority;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.exception.AuthorityBatchProcessingException;
import org.folio.entlinks.integration.internal.MappingRulesService;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChange;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorityMappingRulesProcessingService {

  private final MappingRulesService mappingRulesService;

  public String getTagByAuthorityChange(AuthorityChange authorityChange) throws AuthorityBatchProcessingException {
    var mappingRelations = mappingRulesService.getFieldTargetsMappingRelations();
    return mappingRelations.entrySet().stream()
      .filter(mappingRelation -> mappingRelation.getValue().contains(authorityChange.getFieldName()))
      .findFirst()
      .map(Map.Entry::getKey)
      .orElseThrow(() -> new AuthorityBatchProcessingException(
        "Mapping rules don't contain mapping [field: " + authorityChange.getFieldName() + "]"));
  }
}
