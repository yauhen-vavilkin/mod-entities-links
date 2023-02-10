package org.folio.entlinks.controller;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.domain.dto.AuthorityDataStatActionDto;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.folio.entlinks.domain.repository.AuthorityDataStatRepository;
import org.folio.entlinks.utils.DateUtils;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.spring.tools.client.UsersClient;
import org.folio.spring.tools.model.ResultList;
import org.folio.support.TestUtils;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

@IntegrationTest
class InstanceAuthorityLinkStatisticsIT extends IntegrationTestBase {

  private static final String LINK_STATISTICS_ENDPOINT = "/links/authority/stats";
  private static final OffsetDateTime FROM_DATE = OffsetDateTime.of(2020, 10, 10, 10, 10, 10, 10, ZoneOffset.UTC);
  private static final OffsetDateTime TO_DATE = OffsetDateTime.of(2021, 10, 10, 10, 10, 10, 10, ZoneOffset.UTC);
  private static final Integer LIMIT = 2;
  private static final AuthorityDataStatActionDto STAT_ACTION_DTO = AuthorityDataStatActionDto.UPDATE_HEADING;

  private @MockBean AuthorityDataStatRepository authorityDataStatRepository;
  private @MockBean UsersClient usersClient;

  @Test
  @SneakyThrows
  void getAuthDataStat_positive_whenStatsIsEmpty() {
    var preparedLink = LINK_STATISTICS_ENDPOINT + "?action=" + STAT_ACTION_DTO
      + "&fromDate=" + FROM_DATE
      + "&toDate=" + TO_DATE + "&limit=" + LIMIT;
    doGet(preparedLink)
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  void getAuthDataStat_positive_whenStatsIsNotEmpty() throws Exception {
    UUID userId1 = randomUUID();
    UUID userId2 = randomUUID();
    var list = TestUtils.dataStatList(userId1, userId2, AuthorityDataStatAction.UPDATE_HEADING);
    ResultList<UsersClient.User> userResultList = TestUtils.usersList(List.of(userId1, userId2));
    getWireMock().stubFor(WireMock.get(WireMock.urlEqualTo("users"))
      .willReturn(WireMock.ok(userResultList.toString())));
    when(authorityDataStatRepository.findByActionAndStartedAtGreaterThanEqualAndStartedAtLessThanEqual(
      eq(AuthorityDataStatAction.valueOf(STAT_ACTION_DTO.getValue())),
      eq(DateUtils.toTimestamp(FROM_DATE)),
      eq(DateUtils.toTimestamp(TO_DATE)),
      any()
    )).thenReturn(list);

    var preparedLink = LINK_STATISTICS_ENDPOINT + "?action=" + STAT_ACTION_DTO
      + "&fromDate=" + FROM_DATE
      + "&toDate=" + TO_DATE + "&limit=" + LIMIT;

    doGet(preparedLink)
      .andExpect(status().is2xxSuccessful())
      .andExpect(jsonPath("$.stats[0].action", is(AuthorityDataStatActionDto.UPDATE_HEADING.name())))
      .andExpect(jsonPath("$.stats[0].id", is(list.get(0).getId().toString())))
      .andExpect(jsonPath("$.stats[0].authorityId", is(list.get(0).getAuthorityData().getId().toString())))
      .andExpect(jsonPath("$.stats[0].lbFailed", is(list.get(0).getLbFailed())))
      .andExpect(jsonPath("$.stats[0].lbUpdated", is(list.get(0).getLbUpdated())))
      .andExpect(jsonPath("$.stats[0].lbTotal", is(list.get(0).getLbTotal())))
      .andExpect(jsonPath("$.stats[0].headingOld", is(list.get(0).getHeadingOld())))
      .andExpect(jsonPath("$.stats[0].headingNew", is(list.get(0).getHeadingNew())))
      .andExpect(jsonPath("$.stats[0].headingTypeOld", is(list.get(0).getHeadingTypeOld())))
      .andExpect(jsonPath("$.stats[0].headingTypeNew", is(list.get(0).getHeadingTypeNew())));
  }
}
