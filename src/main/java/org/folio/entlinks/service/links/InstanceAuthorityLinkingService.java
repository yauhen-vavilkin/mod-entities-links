package org.folio.entlinks.service.links;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.projection.LinkCountView;
import org.folio.entlinks.repository.InstanceLinkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceAuthorityLinkingService {

  private final InstanceLinkRepository repository;

  public List<InstanceAuthorityLink> getLinksByInstanceId(UUID instanceId) {
    log.info("Loading links for [instanceId: {}]", instanceId);
    return repository.findByInstanceId(instanceId);
  }

  public Page<InstanceAuthorityLink> getLinksByAuthorityId(UUID authorityId, Pageable pageable) {
    log.info("Loading links for [authorityId: {}, page size: {}, page num: {}]", authorityId,
      pageable.getPageSize(), pageable.getOffset());
    return repository.findByAuthorityId(authorityId, pageable);
  }

  @Transactional
  public void updateLinks(UUID instanceId, List<InstanceAuthorityLink> incomingLinks) {
    if (log.isDebugEnabled()) {
      log.debug("Update links for [instanceId: {}, links: {}]", instanceId, incomingLinks);
    } else {
      log.info("Update links for [instanceId: {}, links amount: {}]", instanceId, incomingLinks.size());
    }
    var existedLinks = repository.findByInstanceId(instanceId);

    var linksToDelete = subtract(existedLinks, incomingLinks);
    var linksToSave = getLinksToSave(incomingLinks, existedLinks, linksToDelete);
    repository.deleteAllInBatch(linksToDelete);
    repository.saveAll(linksToSave);
  }

  public Map<UUID, Long> countLinksByAuthorityIds(Set<UUID> authorityIds) {
    if (log.isDebugEnabled()) {
      log.info("Count links for [authority ids: {}]", authorityIds);
    } else {
      log.info("Count links for [authority ids amount: {}]", authorityIds.size());
    }
    return repository.countLinksByAuthorityIds(authorityIds).stream()
      .collect(Collectors.toMap(LinkCountView::getId, LinkCountView::getTotalLinks));
  }

  public Set<UUID> retainAuthoritiesIdsWithLinks(Set<UUID> authorityIds) {
    var authorityIdsWithLinks = repository.findAuthorityIdsWithLinks(authorityIds);
    var result = new HashSet<>(authorityIds);
    result.retainAll(authorityIdsWithLinks);
    return result;
  }

  @Transactional
  public void updateNaturalId(String naturalId, UUID authorityId) {
    log.info("Update links [authority id: {}, natural id: {}]", authorityId, naturalId);
    repository.updateNaturalId(naturalId, authorityId);
  }

  @Transactional
  public void updateSubfieldsAndNaturalId(char[] subfields, String naturalId, UUID authorityId, String tag) {
    log.info("Update links [authority id: {}, tag: {}, natural id: {}, subfields: {}]",
      authorityId, tag, naturalId, subfields);
    repository.updateSubfieldsAndNaturalId(subfields, naturalId, authorityId, tag);
  }

  @Transactional
  public void deleteByAuthorityIdIn(Set<UUID> authorityIds) {
    if (log.isDebugEnabled()) {
      log.info("Delete links for [authority ids: {}]", authorityIds);
    } else {
      log.info("Delete links for [authority ids amount: {}]", authorityIds.size());
    }
    repository.deleteByAuthorityIdIn(authorityIds);
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
          link.setAuthorityNaturalId(l.getAuthorityNaturalId());
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
