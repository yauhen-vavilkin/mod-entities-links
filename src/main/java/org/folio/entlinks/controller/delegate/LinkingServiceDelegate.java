package org.folio.entlinks.controller.delegate;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.entlinks.utils.DateUtils.fromTimestamp;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.DataStatsMapper;
import org.folio.entlinks.controller.converter.InstanceAuthorityLinkMapper;
import org.folio.entlinks.domain.dto.BibStatsDto;
import org.folio.entlinks.domain.dto.BibStatsDtoCollection;
import org.folio.entlinks.domain.dto.InstanceLinkDto;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.LinksCountDtoCollection;
import org.folio.entlinks.domain.dto.UuidCollection;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.integration.internal.InstanceStorageService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.tenant.domain.dto.Parameter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class LinkingServiceDelegate {

  private final InstanceAuthorityLinkingService linkingService;
  private final InstanceStorageService instanceService;
  private final InstanceAuthorityLinkMapper mapper;
  private final DataStatsMapper statsMapper;

  public InstanceLinkDtoCollection getLinks(UUID instanceId) {
    var links = linkingService.getLinksByInstanceId(instanceId);
    return mapper.convertToDto(links);
  }

  public BibStatsDtoCollection getLinkedBibUpdateStats(OffsetDateTime fromDate, OffsetDateTime toDate,
                                                       LinkStatus status, int limit) {
    validateDateRange(fromDate, toDate);

    var bibStatsCollection = new BibStatsDtoCollection();
    var links = linkingService.getLinks(status, fromDate, toDate, limit + 1);
    log.debug("Retrieved links count {}", links.size());

    if (links.size() > limit) {
      var nextDate = fromTimestamp(links.get(limit).getUpdatedAt());
      bibStatsCollection.setNext(nextDate);
      links = links.subList(0, limit);
    }

    var stats = statsMapper.convertToDto(links);
    fillInstanceTitles(stats);

    return bibStatsCollection.stats(stats);
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

  private void validateDateRange(OffsetDateTime fromDate,
                                 OffsetDateTime toDate) {
    if (isNull(fromDate) || isNull(toDate)) {
      return;
    }
    if (fromDate.isAfter(toDate)) {
      var params = List.of(
        new Parameter().key("fromDate").value(fromDate.toString()),
        new Parameter().key("toDate").value(toDate.toString())
      );
      throw new RequestBodyValidationException("'to' date should be not less than 'from' date.", params);
    }
  }

  private void fillInstanceTitles(List<BibStatsDto> bibStatsList) {
    var instanceIds = bibStatsList.stream()
      .map(BibStatsDto::getInstanceId)
      .map(UUID::toString)
      .distinct()
      .toList();

    var instanceTitles = instanceService.getInstanceTitles(instanceIds);


    bibStatsList.forEach(bibStatsDto -> {
      var instanceId = bibStatsDto.getInstanceId().toString();
      var title = instanceTitles.get(instanceId);

      if (isBlank(title)) {
        log.warn("Title for instance {} is blank", instanceId);
        return;
      }

      bibStatsDto.setInstanceTitle(title);
    });
  }
}
