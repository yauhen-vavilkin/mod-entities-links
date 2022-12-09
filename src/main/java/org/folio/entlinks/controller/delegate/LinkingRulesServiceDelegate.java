package org.folio.entlinks.controller.delegate;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.converter.LinkingRulesMapper;
import org.folio.entlinks.domain.dto.LinkingRuleDto;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkingRulesServiceDelegate {

  private final InstanceAuthorityLinkingRulesService linkingRulesService;
  private final LinkingRulesMapper mapper;

  public List<LinkingRuleDto> getLinkingRules() {
    return mapper.convert(linkingRulesService.getLinkingRules());
  }
}
