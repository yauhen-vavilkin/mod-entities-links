package org.folio.entlinks.service;

import lombok.RequiredArgsConstructor;
import org.folio.entlinks.LinkingPairType;
import org.folio.entlinks.exception.RulesNotFoundException;
import org.folio.entlinks.model.converter.LinkingRulesMapper;
import org.folio.entlinks.repository.LinkingRulesRepository;
import org.folio.qm.domain.dto.LinkingRuleDto;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkingRulesService {

  private static final String LINKING_RULES_PATH_PATTERN = "classpath:linking-rules/%s.json";

  private final LinkingRulesRepository repository;
  private final LinkingRulesMapper mapper;

  public List<LinkingRuleDto> getLinkingRules(LinkingPairType linkingPairType) {
    var jsonRules = repository.findByLinkingPairType(linkingPairType.name());
    return mapper.convert(jsonRules);
  }

  public void saveDefaultRules(LinkingPairType linkingPairType) {
    var jsonRules = readRulesFromResources(linkingPairType);
    var rules = mapper.convert(linkingPairType, jsonRules);

    repository.save(rules);
  }

  private String readRulesFromResources(LinkingPairType linkingPairType) {
    try {
      var rulePath = String.format(LINKING_RULES_PATH_PATTERN, linkingPairType.value());
      var filePath = ResourceUtils.getFile(rulePath).toPath();

      return Files.readString(filePath);
    } catch (IOException e) {
      throw new RulesNotFoundException(linkingPairType);
    }
  }
}
