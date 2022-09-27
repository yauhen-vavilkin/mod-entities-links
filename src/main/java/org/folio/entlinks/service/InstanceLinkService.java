package org.folio.entlinks.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.model.converter.InstanceLinkMapper;
import org.folio.entlinks.model.entity.InstanceLink;
import org.folio.entlinks.repository.InstanceLinkRepository;
import org.folio.qm.domain.dto.InstanceLinkDto;
import org.folio.qm.domain.dto.InstanceLinkDtoCollection;
import org.folio.qm.domain.dto.LinksCountDto;
import org.folio.qm.domain.dto.LinksCountDtoCollection;
import org.folio.qm.domain.dto.UuidCollection;
import org.folio.tenant.domain.dto.Parameter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstanceLinkService {

  private final InstanceLinkRepository repository;
  private final InstanceLinkMapper mapper;

  public InstanceLinkDtoCollection getInstanceLinks(UUID instanceId) {
    var links = repository.findByInstanceId(instanceId);
    return mapper.convert(links);
  }

  @Transactional
  public void updateInstanceLinks(UUID instanceId, @NotNull InstanceLinkDtoCollection instanceLinkCollection) {
    var links = instanceLinkCollection.getLinks();
    validateLinks(instanceId, links);
    var incomingLinks = links.stream().map(mapper::convert).toList();
    var existedLinks = repository.findByInstanceId(instanceId);

    var linksToDelete = subtract(existedLinks, incomingLinks);
    var linksToSave = getLinksToSave(incomingLinks, existedLinks, linksToDelete);
    repository.deleteAllInBatch(linksToDelete);
    repository.saveAll(linksToSave);
  }

  public LinksCountDtoCollection countLinksByAuthorityIds(UuidCollection authorityIdCollection) {
    var ids = authorityIdCollection.getIds();
    var linkCountMap = repository.countLinksByAuthorityIds(ids)
      .stream().map(mapper::convert).toList();

    linkCountMap = fillInMissingIdsWithZeros(linkCountMap, ids);

    return new LinksCountDtoCollection().links(linkCountMap);
  }

  private List<InstanceLink> getLinksToSave(List<InstanceLink> incomingLinks, List<InstanceLink> existedLinks,
                                            List<InstanceLink> linksToDelete) {
    var linksToCreate = subtract(incomingLinks, existedLinks);
    var linksToUpdate = subtract(existedLinks, linksToDelete);
    updateLinksData(incomingLinks, linksToUpdate);
    var linksToSave = new ArrayList<>(linksToCreate);
    linksToSave.addAll(linksToUpdate);
    return linksToSave;
  }

  private List<LinksCountDto> fillInMissingIdsWithZeros(List<LinksCountDto> linksCountMap, List<UUID> ids) {
    var foundIds = linksCountMap.stream().map(LinksCountDto::getId).toList();
    var notFoundIds = ids.stream().filter(uuid -> foundIds.stream().noneMatch(uuid::equals)).toList();

    if (!notFoundIds.isEmpty()) {
      var tempList = new ArrayList<>(linksCountMap);
      notFoundIds.forEach(uuid -> tempList.add(new LinksCountDto().id(uuid).totalLinks(0L)));
      linksCountMap = tempList;
    }
    return linksCountMap;
  }

  private void updateLinksData(List<InstanceLink> incomingLinks, List<InstanceLink> linksToUpdate) {
    linksToUpdate
      .forEach(link -> incomingLinks.stream().filter(l -> l.isSameLink(link)).findFirst()
        .ifPresent(l -> {
          link.setAuthorityNaturalId(l.getAuthorityNaturalId());
          link.setBibRecordSubfields(l.getBibRecordSubfields());
        }));
  }

  private List<InstanceLink> subtract(Collection<InstanceLink> source, Collection<InstanceLink> target) {
    return new LinkedHashSet<>(source).stream()
      .filter(t -> target.stream().noneMatch(link -> link.isSameLink(t)))
      .toList();
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
