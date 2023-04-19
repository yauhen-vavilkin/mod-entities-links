package org.folio.entlinks.controller.delegate;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.converter.LinkingRulesMapper;
import org.folio.entlinks.domain.dto.LinkingRuleDto;
import org.folio.entlinks.domain.dto.LinkingRulePatchRequest;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.tenant.domain.dto.Parameter;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkingRulesServiceDelegate {

  private final InstanceAuthorityLinkingRulesService linkingRulesService;
  private final LinkingRulesMapper mapper;

  public List<LinkingRuleDto> getLinkingRules() {
    return mapper.convert(linkingRulesService.getLinkingRules());
  }

  public LinkingRuleDto getLinkingRuleById(Integer id) {
    return mapper.convert(linkingRulesService.getLinkingRule(id));
  }

  public void patchLinkingRuleById(Integer ruleId, LinkingRulePatchRequest patchRequest) {
    var linkingRule = mapper.convert(patchRequest);
    if (!ruleId.equals(linkingRule.getId())) {
      throw new RequestBodyValidationException("Request should have id = " + ruleId,
        Collections.singletonList(new Parameter().key("id").value(String.valueOf(linkingRule.getId()))));
    }
    linkingRulesService.patchLinkingRule(ruleId, linkingRule);
  }
}
