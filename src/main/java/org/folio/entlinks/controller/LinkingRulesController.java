package org.folio.entlinks.controller;

import static org.folio.entlinks.LinkingPairType.INSTANCE_AUTHORITY;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.service.LinkingRulesService;
import org.folio.qm.domain.dto.LinkingRuleDto;
import org.folio.qm.rest.resource.LinkingRulesApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LinkingRulesController implements LinkingRulesApi {

  private final LinkingRulesService linkingRulesService;

  @Override
  public ResponseEntity<List<LinkingRuleDto>> getInstanceAuthorityLinkingRules() {
    var rules = linkingRulesService.getLinkingRules(INSTANCE_AUTHORITY);
    return ResponseEntity.ok(rules);
  }
}
