package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.controller.converter.AuthorityDataStatMapper;
import org.folio.entlinks.domain.dto.AuthorityDataStatActionDto;
import org.folio.entlinks.domain.dto.AuthorityDataStatDto;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.spring.test.type.UnitTest;
import org.folio.spring.tools.client.UsersClient;
import org.folio.support.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceAuthorityStatServiceDelegateTest {

  private @Mock AuthorityDataStatService statService;
  private @Mock AuthorityDataStatMapper mapper;
  private @Mock UsersClient usersClient;

  private @InjectMocks InstanceAuthorityStatServiceDelegate delegate;

  @BeforeEach
  void setUp() {
    delegate = new InstanceAuthorityStatServiceDelegate(statService, mapper, usersClient);
  }

  @Test
  void fetchStats() {
    var userIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    var statData = List.of(
      TestDataUtils.authorityDataStat(userIds.get(0), AuthorityDataStatAction.UPDATE_HEADING),
      TestDataUtils.authorityDataStat(userIds.get(1), AuthorityDataStatAction.UPDATE_HEADING)
    );
    var users = TestDataUtils.usersList(userIds);

    var fromDate = OffsetDateTime.of(2022, 10, 10, 15, 30, 30, 0, ZoneOffset.UTC);
    var toDate = OffsetDateTime.now();
    var dataStatActionDto = AuthorityDataStatActionDto.UPDATE_HEADING;

    when(statService.fetchDataStats(fromDate, toDate, dataStatActionDto, 3)).thenReturn(statData);
    when(usersClient.query(anyString())).thenReturn(users);
    AuthorityDataStat authorityDataStat1 = statData.get(0);
    AuthorityDataStat authorityDataStat2 = statData.get(1);
    var userList = users.getResult();
    when(mapper.convertToDto(authorityDataStat1))
      .thenReturn(TestDataUtils.getStatDataDto(authorityDataStat1, userList.get(0)));
    when(mapper.convertToDto(authorityDataStat2))
      .thenReturn(TestDataUtils.getStatDataDto(authorityDataStat2, userList.get(0)));

    var authorityChangeStatDtoCollection = delegate.fetchAuthorityLinksStats(
      fromDate,
      toDate,
      dataStatActionDto,
      2
    );

    assertNotNull(authorityChangeStatDtoCollection);
    assertNotNull(authorityChangeStatDtoCollection.getStats());
    assertEquals(2, authorityChangeStatDtoCollection.getStats().size());
    var resultStatDtos = authorityChangeStatDtoCollection.getStats();
    for (AuthorityDataStatDto statDto : resultStatDtos) {
      assertNotNull(statDto.getAction());
      assertNotNull(statDto.getAuthorityId());
      assertNotNull(statDto.getHeadingNew());
      assertNotNull(statDto.getHeadingOld());
      assertNotNull(statDto.getHeadingTypeNew());
      assertNotNull(statDto.getHeadingTypeOld());
      assertNotNull(statDto.getLbFailed());
      assertNotNull(statDto.getLbTotal());
      assertNotNull(statDto.getLbUpdated());
      assertNotNull(statDto.getMetadata());
      assertNotNull(statDto.getMetadata().getStartedByUserId());
      assertNotNull(statDto.getMetadata().getStartedByUserFirstName());
      assertNotNull(statDto.getMetadata().getStartedByUserLastName());
      assertNotNull(statDto.getMetadata().getStartedAt());
      assertNotNull(statDto.getMetadata().getCompletedAt());
      assertNotNull(statDto.getNaturalIdNew());
      assertNotNull(statDto.getNaturalIdOld());
      assertNotNull(statDto.getSourceFileNew());
      assertNotNull(statDto.getSourceFileOld());
    }

    var resultUserIds = authorityChangeStatDtoCollection.getStats()
      .stream()
      .map(org.folio.entlinks.domain.dto.AuthorityDataStatDto::getMetadata)
      .map(org.folio.entlinks.domain.dto.Metadata::getStartedByUserId)
      .toList();
    assertNull(authorityChangeStatDtoCollection.getNext());
    assertThat(userIds).containsAll(resultUserIds);
  }
}
