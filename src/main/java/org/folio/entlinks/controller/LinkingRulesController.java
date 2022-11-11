package org.folio.entlinks.controller;

import lombok.RequiredArgsConstructor;
import org.folio.entlinks.LinkingPairType;
import org.folio.entlinks.service.LinkingRulesService;
import org.folio.qm.domain.dto.LinkingRuleDto;
import org.folio.qm.rest.resource.LinkingRulesApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LinkingRulesController implements LinkingRulesApi {

  private final LinkingRulesService linkingRulesService;

  @Override
  public ResponseEntity<List<LinkingRuleDto>> getInstanceAuthorityLinkingRules() {
    var rules = linkingRulesService.getLinkingRules(LinkingPairType.INSTANCE_AUTHORITY);
    return ResponseEntity.ok(rules);
  }
}
