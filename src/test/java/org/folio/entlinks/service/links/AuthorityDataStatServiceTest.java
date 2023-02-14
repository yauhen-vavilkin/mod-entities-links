package org.folio.entlinks.service.links;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus.ACTUAL;
import static org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus.ERROR;
import static org.folio.support.TestUtils.reports;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.assertj.core.api.SoftAssertions;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatStatus;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.domain.repository.AuthorityDataStatRepository;
import org.folio.spring.test.type.UnitTest;
import org.folio.support.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityDataStatServiceTest {

  private static final String REPORT_ERROR = "error";

  private final Timestamp testStartTime = new Timestamp(System.currentTimeMillis());

  @Mock
  private AuthorityDataStatRepository statRepository;
  @Mock
  private InstanceAuthorityLinkingService linkingService;
  @InjectMocks
  private AuthorityDataStatService service;

  @Captor
  private ArgumentCaptor<List<InstanceAuthorityLink>> linksCaptor;
  @Captor
  private ArgumentCaptor<AuthorityDataStat> dataStatCaptor;

  @BeforeEach
  void prepareMocks() {
    when(linkingService.getLinksByIds(anyList()))
      .thenReturn(emptyList());
    when(statRepository.findById(any()))
      .thenReturn(Optional.of(new AuthorityDataStat()));
  }

  @Test
  void updateForReports_positive_updateLinks_forSuccess() {
    var jobId = UUID.randomUUID();
    var reports = reports(jobId);

    when(linkingService.getLinksByIds(anyList())).thenReturn(TestUtils.links(2, REPORT_ERROR));

    service.updateForReports(jobId, reports);

    verify(linkingService).saveAll(eq(reports.get(0).getInstanceId()), linksCaptor.capture());
    var links = linksCaptor.getValue();
    assertThat(links)
      .anySatisfy(linkAsserter(ACTUAL, null));

    verify(linkingService).saveAll(eq(reports.get(1).getInstanceId()), linksCaptor.capture());
    links = linksCaptor.getValue();
    assertThat(links)
      .anySatisfy(linkAsserter(ACTUAL, null));
  }

  @Test
  void updateForReports_positive_updateLinks_forFail() {
    var jobId = UUID.randomUUID();
    var reports = reports(jobId, LinkUpdateReport.StatusEnum.FAIL, REPORT_ERROR);

    when(linkingService.getLinksByIds(anyList())).thenReturn(TestUtils.links(2));

    service.updateForReports(jobId, reports);

    verify(linkingService).saveAll(eq(reports.get(0).getInstanceId()), linksCaptor.capture());
    var links = linksCaptor.getValue();
    assertThat(links)
      .anySatisfy(linkAsserter(ERROR, REPORT_ERROR));

    verify(linkingService).saveAll(eq(reports.get(1).getInstanceId()), linksCaptor.capture());
    links = linksCaptor.getValue();
    assertThat(links)
      .anySatisfy(linkAsserter(ERROR, REPORT_ERROR));
  }

  @Test
  void updateForReports_positive_updateStatsData_inProgress() {
    var jobId = UUID.randomUUID();
    var failReports = reports(jobId, LinkUpdateReport.StatusEnum.FAIL, REPORT_ERROR);
    var successReports = reports(jobId);
    var reports = new LinkedList<>(successReports);
    reports.addAll(failReports);

    mockDataStat(reports.size() + 1);

    service.updateForReports(jobId, reports);

    verify(statRepository).save(dataStatCaptor.capture());
    var dataStat = dataStatCaptor.getValue();

    assertDataStat(dataStat,
      successReports.size(), failReports.size(), AuthorityDataStatStatus.IN_PROGRESS);
  }

  @Test
  void updateForReports_positive_updateStatsData_completeSuccess() {
    var jobId = UUID.randomUUID();
    var reports = reports(jobId);

    mockDataStat(reports.size());

    service.updateForReports(jobId, reports);

    verify(statRepository).save(dataStatCaptor.capture());
    var dataStat = dataStatCaptor.getValue();

    assertDataStat(dataStat, reports.size(), 0, AuthorityDataStatStatus.COMPLETED_SUCCESS);
  }

  @Test
  void updateForReports_positive_updateStatsData_completeWithErrors() {
    var jobId = UUID.randomUUID();
    var failReports = reports(jobId, LinkUpdateReport.StatusEnum.FAIL, REPORT_ERROR);
    var successReports = reports(jobId);
    var reports = new LinkedList<>(successReports);
    reports.addAll(failReports);

    mockDataStat(reports.size());

    service.updateForReports(jobId, reports);

    verify(statRepository).save(dataStatCaptor.capture());
    var dataStat = dataStatCaptor.getValue();

    assertDataStat(dataStat,
      successReports.size(), failReports.size(), AuthorityDataStatStatus.COMPLETED_WITH_ERRORS);
  }

  @Test
  void updateForReports_positive_updateStatsData_completeFail() {
    var jobId = UUID.randomUUID();
    var reports = reports(jobId, LinkUpdateReport.StatusEnum.FAIL, REPORT_ERROR);

    mockDataStat(reports.size());

    service.updateForReports(jobId, reports);

    verify(statRepository).save(dataStatCaptor.capture());
    var dataStat = dataStatCaptor.getValue();

    assertDataStat(dataStat, 0, reports.size(), AuthorityDataStatStatus.FAILED);
  }

  private Consumer<InstanceAuthorityLink> linkAsserter(InstanceAuthorityLinkStatus status, String errorCause) {
    return link -> {
      assertThat(link.getStatus()).isEqualTo(status);
      assertThat(link.getErrorCause()).isEqualTo(errorCause);
    };
  }

  private void assertDataStat(AuthorityDataStat dataStat, int updated, int failed, AuthorityDataStatStatus status) {
    var softAssertions = new SoftAssertions();

    softAssertions.assertThat(dataStat.getLbUpdated())
      .isEqualTo(updated);
    softAssertions.assertThat(dataStat.getLbFailed())
      .isEqualTo(failed);
    softAssertions.assertThat(dataStat.getFailCause())
      .isBlank();
    softAssertions.assertThat(dataStat.getStatus())
      .isEqualTo(status);
    if (status.equals(AuthorityDataStatStatus.IN_PROGRESS)) {
      softAssertions.assertThat(dataStat.getCompletedAt())
        .isNull();
    } else {
      softAssertions.assertThat(dataStat.getCompletedAt())
        .isAfter(testStartTime);
    }

    softAssertions.assertAll();
  }

  private void mockDataStat(int lbTotal) {
    when(statRepository.findById(any()))
      .thenReturn(Optional.of(AuthorityDataStat.builder()
        .status(AuthorityDataStatStatus.IN_PROGRESS)
        .lbTotal(lbTotal)
        .build()));
  }

}
