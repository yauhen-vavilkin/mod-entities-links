package org.folio.entlinks.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.LinkingRulesServiceDelegate;
import org.folio.entlinks.domain.dto.LinkingRuleDto;
import org.folio.entlinks.domain.dto.LinkingRulePatchRequest;
import org.folio.entlinks.rest.resource.LinkingRulesApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LinkingRulesController implements LinkingRulesApi {

  private final LinkingRulesServiceDelegate serviceDelegate;

  @Override
  public ResponseEntity<LinkingRuleDto> getInstanceAuthorityLinkingRuleById(Integer id) {
    return ResponseEntity.ok(serviceDelegate.getLinkingRuleById(id));
  }

  @Override
  public ResponseEntity<List<LinkingRuleDto>> getInstanceAuthorityLinkingRules() {
    var rules = serviceDelegate.getLinkingRules();
    return ResponseEntity.ok(rules);
  }

  @Override
  public ResponseEntity<LinkingRuleDto> patchInstanceAuthorityLinkingRuleById(Integer ruleId,
                                                                              LinkingRulePatchRequest patchRequest) {
    serviceDelegate.patchLinkingRuleById(ruleId, patchRequest);
    return ResponseEntity.accepted().build();
  }
}
