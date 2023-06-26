package org.folio.entlinks.service.links;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.folio.entlinks.utils.DateUtils.toTimestamp;
import static org.folio.entlinks.utils.LinkEventsUtils.groupLinksByAuthorityId;

import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.client.SearchClient;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.domain.dto.Authority;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.domain.dto.StrippedParsedRecord;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.entity.projection.LinkCountView;
import org.folio.entlinks.domain.repository.InstanceLinkRepository;
import org.folio.entlinks.exception.DeletedLinkingAuthorityException;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceAuthorityLinkingService {

  private static final String SEEK_FIELD = "updatedAt";

  private final InstanceLinkRepository instanceLinkRepository;
  private final InstanceAuthorityLinkingRulesService linkingRulesService;
  private final AuthorityRuleValidationService authorityRuleValidationService;
  private final AuthorityDataService authorityDataService;
  private final RenovateLinksService renovateService;
  private final SearchClient searchClient;
  private final SourceStorageClient sourceStorageClient;
  private final EventProducer<LinksChangeEvent> eventProducer;

  public List<InstanceAuthorityLink> getLinksByInstanceId(UUID instanceId) {
    log.info("Loading links for [instanceId: {}]", instanceId);
    return instanceLinkRepository.findByInstanceId(instanceId);
  }

  public Page<InstanceAuthorityLink> getLinksByAuthorityId(UUID authorityId, Pageable pageable) {
    log.info("Loading links for [authorityId: {}, page size: {}, page num: {}]", authorityId,
      pageable.getPageSize(), pageable.getOffset());
    return instanceLinkRepository.findByAuthorityId(authorityId, pageable);
  }

  public List<InstanceAuthorityLink> getLinksByIds(List<Integer> ids) {
    log.info("Retrieving links by ids [{}]", ids);
    var longIds = ids.stream()
      .filter(Objects::nonNull)
      .mapToLong(Integer::longValue)
      .boxed()
      .toList();
    return instanceLinkRepository.findAllById(longIds);
  }

  @Transactional
  public void updateLinks(UUID instanceId, List<InstanceAuthorityLink> incomingLinks) {
    if (log.isDebugEnabled()) {
      log.debug("Update/renovate links for [instanceId: {}, links: {}]", instanceId, incomingLinks);
    } else {
      log.info("Update/renovate links for [instanceId: {}, links amount: {}]", instanceId, incomingLinks.size());
    }
    var authorityData = collectAuthorityDataById(incomingLinks);
    checkForDeletedAuthorities(authorityData.keySet());
    fillLinksWithLinkingRules(incomingLinks);
    var linksByAuthorityId = groupLinksByAuthorityId(incomingLinks);

    var authorityNaturalIds = fetchAuthorityNaturalIds(authorityData.keySet());
    var authoritySources = fetchAuthoritySources(linksByAuthorityId.keySet());

    var validationResult = authorityRuleValidationService
      .validateAuthorityData(linksByAuthorityId, authorityData, authorityNaturalIds, authoritySources);

    var savedAuthorityData = authorityDataService.saveAll(validationResult.validAuthorities());
    var incomingValidLinks = validationResult.validLinks();
    var existedLinks = instanceLinkRepository.findByInstanceId(instanceId);
    var linksToDelete = subtract(existedLinks, incomingValidLinks);

    updateExistingLinks(incomingValidLinks, existedLinks, savedAuthorityData);
    instanceLinkRepository.saveAll(incomingValidLinks);
    instanceLinkRepository.deleteAllInBatch(linksToDelete);

    sendEvents(instanceId, renovateService.renovateBibs(instanceId, authoritySources, validationResult));
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
  public void updateStatus(UUID authorityId, InstanceAuthorityLinkStatus status, String errorCause) {
    log.info("Update links [authority id: {}, status: {}, errorCause: {}]", authorityId, status, errorCause);
    instanceLinkRepository.updateStatusAndErrorCauseByAuthorityId(status, trimToNull(errorCause), authorityId);
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

  @Transactional
  public void saveAll(UUID instanceId, List<InstanceAuthorityLink> links) {
    log.info("Save links for [instanceId: {}, links amount: {}]", instanceId, links.size());
    log.debug("Save links for [instanceId: {}, links: {}]", instanceId, links);

    instanceLinkRepository.saveAll(links);
  }

  public List<InstanceAuthorityLink> getLinks(LinkStatus status, OffsetDateTime fromDate,
                                              OffsetDateTime toDate, int limit) {
    log.info("Fetching links for [status: {}, fromDate: {}, toDate: {}, limit: {}]",
      status, fromDate, toDate, limit);

    var linkStatus = status == null ? null : InstanceAuthorityLinkStatus.valueOf(status.getValue());
    var linkFromDate = fromDate == null ? null : toTimestamp(fromDate);
    var linkToDate = toDate == null ? null : toTimestamp(toDate);
    var pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc(SEEK_FIELD)));

    var specification = getSpecFromStatusAndDates(linkStatus, linkFromDate, linkToDate);
    return instanceLinkRepository.findAll(specification, pageable).getContent();
  }

  private List<InstanceAuthorityLink> subtract(Collection<InstanceAuthorityLink> source,
                                               Collection<InstanceAuthorityLink> target) {
    return new LinkedHashSet<>(source).stream()
      .filter(t -> target.stream().noneMatch(link -> link.isSameLink(t)))
      .toList();
  }

  private Specification<InstanceAuthorityLink> getSpecFromStatusAndDates(
    InstanceAuthorityLinkStatus status, Timestamp from, Timestamp to) {

    return (root, query, builder) -> {
      var predicates = new LinkedList<>();

      if (status != null) {
        predicates.add(builder.equal(root.get("status"), status));
      }
      if (from != null) {
        predicates.add(builder.greaterThanOrEqualTo(root.get(SEEK_FIELD), from));
      }
      if (to != null) {
        predicates.add(builder.lessThanOrEqualTo(root.get(SEEK_FIELD), to));
      }

      return builder.and(predicates.toArray(new Predicate[0]));
    };
  }

  private void updateExistingLinks(List<InstanceAuthorityLink> incomingValidLinks,
                                   List<InstanceAuthorityLink> existedLinks,
                                   Map<UUID, AuthorityData> savedAuthorityData) {
    for (InstanceAuthorityLink incomingLink : incomingValidLinks) {
      AuthorityData linkAuthorityData = incomingLink.getAuthorityData();
      AuthorityData authorityData = savedAuthorityData.get(linkAuthorityData.getId());
      incomingLink.setAuthorityData(authorityData);
      existedLinks.stream()
        .filter(existedLink -> existedLink.isSameLink(incomingLink))
        .findFirst()
        .ifPresent(existedLink -> incomingLink.setId(existedLink.getId()));
    }
  }

  private Map<UUID, AuthorityData> collectAuthorityDataById(List<InstanceAuthorityLink> incomingLinks) {
    return incomingLinks.stream()
      .map(InstanceAuthorityLink::getAuthorityData)
      .collect(Collectors.toMap(AuthorityData::getId, Function.identity(), (a1, a2) -> a1));
  }

  private Map<UUID, String> fetchAuthorityNaturalIds(Set<UUID> authorityIds) {
    if (authorityIds.isEmpty()) {
      return emptyMap();
    }
    var searchQuery = searchClient.buildIdsQuery(authorityIds);
    return searchClient.searchAuthorities(searchQuery, false)
      .getAuthorities().stream()
      .collect(Collectors.toMap(Authority::getId, Authority::getNaturalId));
  }

  private List<StrippedParsedRecord> fetchAuthoritySources(Set<UUID> authorityIds) {
    if (authorityIds.isEmpty()) {
      return emptyList();
    }
    var authorityFetchRequest = sourceStorageClient.buildBatchFetchRequestForAuthority(authorityIds,
      linkingRulesService.getMinAuthorityField(),
      linkingRulesService.getMaxAuthorityField());
    return sourceStorageClient.fetchParsedRecordsInBatch(authorityFetchRequest).getRecords();
  }

  private void sendEvents(UUID instanceId, List<LinksChangeEvent> events) {
    if (isNotEmpty(events)) {
      log.info("Sending {} events for instanceId {} to Kafka for links renovation process.", instanceId, events.size());
      eventProducer.sendMessages(events);
    }
  }

  private Map<Integer, InstanceAuthorityLinkingRule> rulesToIdMap(List<InstanceAuthorityLinkingRule> rules) {
    return rules.stream().collect(Collectors.toMap(InstanceAuthorityLinkingRule::getId, Function.identity()));
  }

  private void fillLinksWithLinkingRules(List<InstanceAuthorityLink> incomingLinks) {
    var linkingRules = rulesToIdMap(linkingRulesService.getLinkingRules());

    incomingLinks.forEach(link -> link.setLinkingRule(linkingRules.get(link.getLinkingRule().getId())));
  }

  private void checkForDeletedAuthorities(Set<UUID> authorityIds) {
    var deletedAuthorityIds = authorityDataService
      .getByIdAndDeleted(authorityIds, true).stream()
      .map(authorityData -> authorityData.getId().toString())
      .collect(Collectors.toSet());

    if (isNotEmpty(deletedAuthorityIds)) {
      throw new DeletedLinkingAuthorityException(deletedAuthorityIds);
    }
  }
}
