package org.folio.entlinks.service.links;

import static org.folio.entlinks.domain.dto.LinkUpdateReport.StatusEnum.FAIL;
import static org.folio.entlinks.domain.dto.LinkUpdateReport.StatusEnum.SUCCESS;
import static org.folio.entlinks.utils.DateUtils.currentTs;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.dto.LinkAction;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.folio.entlinks.domain.entity.AuthorityDataStatStatus;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.domain.repository.AuthorityDataStatRepository;
import org.folio.entlinks.utils.DateUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorityDataStatService {

  private final AuthorityDataStatRepository statRepository;

  private final AuthorityDataService authorityDataService;
  private final InstanceAuthorityLinkingService linkingService;

  public List<AuthorityDataStat> createInBatch(List<AuthorityDataStat> stats) {
    var authorityDataSet = stats.stream()
      .map(AuthorityDataStat::getAuthorityData)
      .collect(Collectors.toSet());
    var savedAuthorityData = authorityDataService.saveAll(authorityDataSet);

    for (AuthorityDataStat stat : stats) {
      stat.setId(UUID.randomUUID());
      stat.setStatus(AuthorityDataStatStatus.IN_PROGRESS);
      var authorityData = savedAuthorityData.get(stat.getAuthorityData().getId());
      stat.setAuthorityData(authorityData);
    }

    return statRepository.saveAll(stats);
  }

  public List<AuthorityDataStat> fetchDataStats(OffsetDateTime fromDate, OffsetDateTime toDate,
                                                LinkAction action, int limit) {
    Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("startedAt")));
    return statRepository.findActualByActionAndDate(AuthorityDataStatAction.valueOf(action.getValue()),
      DateUtils.toTimestamp(fromDate), DateUtils.toTimestamp(toDate), pageable);
  }

  @Transactional
  public void updateForReports(UUID jobId, List<LinkUpdateReport> reports) {
    log.info("Updating links, stats for reports: [jobId: {}, reports count: {}]", jobId, reports.size());
    log.debug("Updating links,stats for reports: [reports: {}]", reports);
    updateLinks(jobId, reports);
    updateStatsData(jobId, reports);
  }

  private void checkIfAllFailed(List<LinkUpdateReport> reports, AuthorityDataStat dataStat) {
    reports.stream()
      .filter(linkUpdateReport -> CollectionUtils.isEmpty(linkUpdateReport.getLinkIds())
        && linkUpdateReport.getStatus().equals(FAIL))
      .findFirst()
      .ifPresent(linkUpdateReport -> dataStat.setLbFailed(dataStat.getLbTotal()));
  }

  private void updateLinks(UUID jobId, List<LinkUpdateReport> reports) {
    reports.forEach(report -> {
      var linkIds = report.getLinkIds();
      var status = mapReportStatus(report);
      log.debug("Update links status for [status: {}, linkIds: {}, jobId: {}]", status, linkIds, jobId);
      if (CollectionUtils.isNotEmpty(linkIds)) {
        var links = linkingService.getLinksByIds(linkIds);

        links.forEach(link -> {
          link.setStatus(status);
          link.setErrorCause(StringUtils.trimToNull(report.getFailCause()));
        });

        linkingService.saveAll(report.getInstanceId(), links);
      } else {
        var dataStat = getDataStatOrFail(jobId);
        var authorityId = dataStat.getAuthorityData().getId();
        linkingService.updateStatus(authorityId, status, report.getFailCause());
      }
    });
  }

  /**
   * Updates authority statistics data.
   *
   * @param jobId linked bib update job id.
   *              AuthorityDataStat id and jobId are interchangeable (jobId is used as id to create stat record)
   */
  private void updateStatsData(UUID jobId, List<LinkUpdateReport> reports) {
    var dataStat = getDataStatOrFail(jobId);

    var failedCount = getReportCountForStatus(reports, FAIL);
    var successCount = getReportCountForStatus(reports, SUCCESS);

    dataStat.setLbUpdated(dataStat.getLbUpdated() + successCount);
    dataStat.setLbFailed(dataStat.getLbFailed() + failedCount);

    checkIfAllFailed(reports, dataStat);

    var jobCompleted = dataStat.getLbUpdated() + dataStat.getLbFailed() == dataStat.getLbTotal();
    if (jobCompleted) {
      dataStat.setCompletedAt(currentTs());
      updateStatStatus(dataStat);
    }

    log.info("Saving stats data [statsId: {}, status: {}]", dataStat.getId(), dataStat.getStatus());
    log.debug("Stats data: {}", dataStat);
    statRepository.save(dataStat);
  }

  private int getReportCountForStatus(List<LinkUpdateReport> reports, LinkUpdateReport.StatusEnum status) {
    return (int) reports.stream()
      .filter(linkUpdateReport -> CollectionUtils.isNotEmpty(linkUpdateReport.getLinkIds()))
      .filter(linkUpdateReport -> linkUpdateReport.getStatus().equals(status))
      .count();
  }

  private void updateStatStatus(AuthorityDataStat dataStat) {
    if (dataStat.getLbFailed() == 0) {
      dataStat.setStatus(AuthorityDataStatStatus.COMPLETED_SUCCESS);
    } else if (dataStat.getLbFailed() == dataStat.getLbTotal()) {
      dataStat.setStatus(AuthorityDataStatStatus.FAILED);
    } else {
      dataStat.setStatus(AuthorityDataStatStatus.COMPLETED_WITH_ERRORS);
    }
  }

  private InstanceAuthorityLinkStatus mapReportStatus(LinkUpdateReport report) {
    return switch (report.getStatus()) {
      case SUCCESS -> InstanceAuthorityLinkStatus.ACTUAL;
      case FAIL -> InstanceAuthorityLinkStatus.ERROR;
    };
  }

  private AuthorityDataStat getDataStatOrFail(UUID jobId) {
    return statRepository.findById(jobId)
      .orElseThrow(() -> new IllegalStateException("Cannot find authority data statistics for id: " + jobId));
  }

}
