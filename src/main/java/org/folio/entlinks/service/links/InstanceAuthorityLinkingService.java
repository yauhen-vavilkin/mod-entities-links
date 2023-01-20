package org.folio.entlinks.service.links;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.projection.LinkCountView;
import org.folio.entlinks.domain.repository.InstanceLinkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceAuthorityLinkingService {

  private final InstanceLinkRepository instanceLinkRepository;
  private final AuthorityDataService authorityDataService;

  public List<InstanceAuthorityLink> getLinksByInstanceId(UUID instanceId) {
    log.info("Loading links for [instanceId: {}]", instanceId);
    return instanceLinkRepository.findByInstanceId(instanceId);
  }

  public Page<InstanceAuthorityLink> getLinksByAuthorityId(UUID authorityId, Pageable pageable) {
    log.info("Loading links for [authorityId: {}, page size: {}, page num: {}]", authorityId,
      pageable.getPageSize(), pageable.getOffset());
    return instanceLinkRepository.findByAuthorityId(authorityId, pageable);
  }

  @Transactional
  public void updateLinks(UUID instanceId, List<InstanceAuthorityLink> incomingLinks) {
    if (log.isDebugEnabled()) {
      log.debug("Update links for [instanceId: {}, links: {}]", instanceId, incomingLinks);
    } else {
      log.info("Update links for [instanceId: {}, links amount: {}]", instanceId, incomingLinks.size());
    }

    var authorityDataSet = incomingLinks.stream()
      .map(InstanceAuthorityLink::getAuthorityData)
      .collect(Collectors.toSet());

    var existedAuthorityData = authorityDataService.saveAll(authorityDataSet);

    for (InstanceAuthorityLink incomingLink : incomingLinks) {
      var linkAuthorityData = incomingLink.getAuthorityData();
      var authorityData = existedAuthorityData.get(linkAuthorityData.getId());
      incomingLink.setAuthorityData(authorityData);
    }
    var existedLinks = instanceLinkRepository.findByInstanceId(instanceId);

    var linksToDelete = subtract(existedLinks, incomingLinks);
    var linksToSave = getLinksToSave(incomingLinks, existedLinks, linksToDelete);
    instanceLinkRepository.deleteAllInBatch(linksToDelete);
    instanceLinkRepository.saveAll(linksToSave);
  }

  public Map<UUID, Integer> countLinksByAuthorityIds(Set<UUID> authorityIds) {
    if (log.isDebugEnabled()) {
      log.info("Count links for [authority ids: {}]", authorityIds);
    } else {
      log.info("Count links for [authority ids amount: {}]", authorityIds.size());
    }
    return instanceLinkRepository.countLinksByAuthorityIds(authorityIds).stream()
      .collect(Collectors.toMap(LinkCountView::getId, LinkCountView::getTotalLinks));
  }

  @Transactional
  public void updateSubfields(char[] subfields, UUID authorityId, String tag) {
    log.info("Update links [authority id: {}, tag: {}, subfields: {}]", authorityId, tag, subfields);
    instanceLinkRepository.updateSubfieldsByAuthorityIdAndTag(subfields, authorityId, tag);
  }

  @Transactional
  public void deleteByAuthorityIdIn(Set<UUID> authorityIds) {
    if (log.isDebugEnabled()) {
      log.info("Delete links for [authority ids: {}]", authorityIds);
    } else {
      log.info("Delete links for [authority ids amount: {}]", authorityIds.size());
    }
    instanceLinkRepository.deleteByAuthorityIds(authorityIds);
    authorityDataService.markDeleted(authorityIds);
  }

  private List<InstanceAuthorityLink> getLinksToSave(List<InstanceAuthorityLink> incomingLinks,
                                                     List<InstanceAuthorityLink> existedLinks,
                                                     List<InstanceAuthorityLink> linksToDelete) {
    var linksToCreate = subtract(incomingLinks, existedLinks);
    var linksToUpdate = subtract(existedLinks, linksToDelete);
    updateLinksData(incomingLinks, linksToUpdate);
    var linksToSave = new ArrayList<>(linksToCreate);
    linksToSave.addAll(linksToUpdate);
    return linksToSave;
  }

  private void updateLinksData(List<InstanceAuthorityLink> incomingLinks, List<InstanceAuthorityLink> linksToUpdate) {
    linksToUpdate
      .forEach(link -> incomingLinks.stream().filter(l -> l.isSameLink(link)).findFirst()
        .ifPresent(l -> {
          link.getAuthorityData().setNaturalId(l.getAuthorityData().getNaturalId());
          link.setBibRecordSubfields(l.getBibRecordSubfields());
        }));
  }

  private List<InstanceAuthorityLink> subtract(Collection<InstanceAuthorityLink> source,
                                               Collection<InstanceAuthorityLink> target) {
    return new LinkedHashSet<>(source).stream()
      .filter(t -> target.stream().noneMatch(link -> link.isSameLink(t)))
      .toList();
  }

}
