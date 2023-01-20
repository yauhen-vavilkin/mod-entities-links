package org.folio.entlinks.controller.delegate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.converter.InstanceAuthorityLinkMapper;
import org.folio.entlinks.domain.dto.InstanceLinkDto;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.dto.LinksCountDtoCollection;
import org.folio.entlinks.domain.dto.UuidCollection;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.tenant.domain.dto.Parameter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkingServiceDelegate {

  private final InstanceAuthorityLinkingService linkingService;
  private final InstanceAuthorityLinkMapper mapper;

  public InstanceLinkDtoCollection getLinks(UUID instanceId) {
    var links = linkingService.getLinksByInstanceId(instanceId);
    return mapper.convertToDto(links);
  }

  public void updateLinks(UUID instanceId, @NotNull InstanceLinkDtoCollection instanceLinkCollection) {
    var links = instanceLinkCollection.getLinks();
    validateLinks(instanceId, links);
    var incomingLinks = mapper.convertDto(links);
    linkingService.updateLinks(instanceId, incomingLinks);
  }

  public LinksCountDtoCollection countLinksByAuthorityIds(UuidCollection authorityIdCollection) {
    var ids = new HashSet<>(authorityIdCollection.getIds());
    var linkCountMap = fillInMissingIdsWithZeros(linkingService.countLinksByAuthorityIds(ids), ids);

    return new LinksCountDtoCollection().links(mapper.convert(linkCountMap));
  }

  private Map<UUID, Integer> fillInMissingIdsWithZeros(Map<UUID, Integer> linksCountMap, HashSet<UUID> ids) {
    var result = new HashMap<>(linksCountMap);
    for (UUID id : ids) {
      result.putIfAbsent(id, 0);
    }
    return result;
  }

  private void validateLinks(UUID instanceId, List<InstanceLinkDto> links) {
    validateInstanceId(instanceId, links);
    validateSubfields(links);
  }

  private void validateSubfields(List<InstanceLinkDto> links) {
    var invalidSubfields = links.stream()
      .map(InstanceLinkDto::getBibRecordSubfields)
      .flatMap(List::stream)
      .filter(subfield -> subfield.length() != 1)
      .map(invalidSubfield -> new Parameter().key("bibRecordSubfields").value(invalidSubfield))
      .toList();

    if (!invalidSubfields.isEmpty()) {
      throw new RequestBodyValidationException("Max Bib record subfield length is 1", invalidSubfields);
    }
  }

  private void validateInstanceId(UUID instanceId, List<InstanceLinkDto> links) {
    var invalidParams = links.stream()
      .map(InstanceLinkDto::getInstanceId)
      .filter(targetId -> !targetId.equals(instanceId))
      .map(targetId -> new Parameter().key("instanceId").value(targetId.toString()))
      .toList();
    if (!invalidParams.isEmpty()) {
      throw new RequestBodyValidationException("Link should have instanceId = " + instanceId, invalidParams);
    }
  }
}
