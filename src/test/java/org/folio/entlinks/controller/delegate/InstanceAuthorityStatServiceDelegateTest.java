package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.INPUT_BASE_URL;
import static org.folio.support.base.TestConstants.TEST_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.controller.converter.DataStatsMapper;
import org.folio.entlinks.domain.dto.AuthorityControlMetadata;
import org.folio.entlinks.domain.dto.AuthorityStatsDto;
import org.folio.entlinks.domain.dto.LinkAction;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.spring.client.UsersClient;
import org.folio.spring.model.ResultList;
import org.folio.spring.testing.type.UnitTest;
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

  private static final UUID USER_ID_1 = UUID.randomUUID();
  private static final UUID USER_ID_2 = UUID.randomUUID();
  private static final UUID SOURCE_FILE_ID = UUID.randomUUID();
  private static final String BASE_URL = "baseUrl";
  private static final String SOURCE_FILE_NAME = "sourceFileName";
  private static final LocalDateTime NOW = LocalDateTime.now();
  private static final OffsetDateTime FROM_DATE = OffsetDateTime.of(NOW.with(LocalTime.MIN), ZoneOffset.UTC);
  private static final OffsetDateTime TO_DATE = OffsetDateTime.of(NOW.with(LocalTime.MAX), ZoneOffset.UTC);
  private static final LinkAction DATA_STAT_ACTION = LinkAction.UPDATE_HEADING;
  private static final int LIMIT_SIZE = 2;

  private @Mock AuthorityDataStatService statService;
  private @Mock AuthoritySourceFileRepository sourceFileRepository;
  private @Mock DataStatsMapper mapper;
  private @Mock UsersClient usersClient;
  private @InjectMocks InstanceAuthorityStatServiceDelegate delegate;

  private AuthoritySourceFile sourceFile;

  @BeforeEach
  void setUp() {
    delegate = new InstanceAuthorityStatServiceDelegate(statService, mapper, usersClient, sourceFileRepository);
    sourceFile = new AuthoritySourceFile();
    sourceFile.setId(TEST_ID);
    sourceFile.setBaseUrl(INPUT_BASE_URL);
    sourceFile.setName(SOURCE_FILE_NAME);
    var sourceFileCode = new AuthoritySourceFileCode();
    sourceFileCode.setCode("e1");
    sourceFile.addCode(sourceFileCode);

    var statData = List.of(
      TestDataUtils.authorityDataStat(USER_ID_1, TEST_ID, AuthorityDataStatAction.UPDATE_HEADING),
      TestDataUtils.authorityDataStat(USER_ID_2, TEST_ID, AuthorityDataStatAction.UPDATE_HEADING)
    );
    var users = TestDataUtils.usersList(List.of(USER_ID_1, USER_ID_2));

    when(statService.fetchDataStats(FROM_DATE, TO_DATE, DATA_STAT_ACTION, 3)).thenReturn(statData);
    when(usersClient.query(anyString())).thenReturn(users);

    AuthorityDataStat authorityDataStat1 = statData.get(0);
    AuthorityDataStat authorityDataStat2 = statData.get(1);
    var userList = users.getResult();
    when(mapper.convertToDto(authorityDataStat1))
      .thenReturn(TestDataUtils.getStatDataDto(authorityDataStat1, userList.get(0)));
    when(mapper.convertToDto(authorityDataStat2))
      .thenReturn(TestDataUtils.getStatDataDto(authorityDataStat2, userList.get(0)));
  }

  @Test
  void fetchStats() {
    //  WHEN
    when(sourceFileRepository.findById(any(UUID.class))).thenReturn(Optional.of(sourceFile));
    var authorityChangeStatDtoCollection = delegate
      .fetchAuthorityLinksStats(FROM_DATE, TO_DATE, DATA_STAT_ACTION, LIMIT_SIZE);

    //  THEN
    assertNotNull(authorityChangeStatDtoCollection);
    assertNotNull(authorityChangeStatDtoCollection.getStats());
    assertEquals(LIMIT_SIZE, authorityChangeStatDtoCollection.getStats().size());
    var resultStatDtos = authorityChangeStatDtoCollection.getStats();
    for (AuthorityStatsDto statDto : resultStatDtos) {
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
      assertEquals(sourceFile.getName(), statDto.getSourceFileOld());
      assertEquals(sourceFile.getName(), statDto.getSourceFileNew());
    }

    var resultUserIds = authorityChangeStatDtoCollection.getStats()
      .stream()
      .map(AuthorityStatsDto::getMetadata)
      .map(AuthorityControlMetadata::getStartedByUserId)
      .toList();
    assertNull(authorityChangeStatDtoCollection.getNext());
    assertThat(List.of(USER_ID_1, USER_ID_2)).containsAll(resultUserIds);
  }

  @Test
  void fetchStats_whenUpdatedUserIsNull() {
    //  WHEN
    when(sourceFileRepository.findById(any(UUID.class))).thenReturn(Optional.of(sourceFile));
    when(usersClient.query(anyString())).thenReturn(ResultList.of(0, null));

    var authorityChangeStatDtoCollection = delegate
      .fetchAuthorityLinksStats(FROM_DATE, TO_DATE, DATA_STAT_ACTION, LIMIT_SIZE);

    //  THEN
    assertNotNull(authorityChangeStatDtoCollection);
    assertNotNull(authorityChangeStatDtoCollection.getStats());
    assertEquals(LIMIT_SIZE, authorityChangeStatDtoCollection.getStats().size());
    var resultStatDtos = authorityChangeStatDtoCollection.getStats();
    for (AuthorityStatsDto statDto  : resultStatDtos) {
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
      assertNull(statDto.getMetadata().getStartedByUserFirstName());
      assertNull(statDto.getMetadata().getStartedByUserLastName());
      assertNotNull(statDto.getMetadata().getStartedAt());
      assertNotNull(statDto.getMetadata().getCompletedAt());
      assertNotNull(statDto.getNaturalIdNew());
      assertNotNull(statDto.getNaturalIdOld());
      assertEquals(sourceFile.getName(), statDto.getSourceFileOld());
      assertEquals(sourceFile.getName(), statDto.getSourceFileNew());
    }
  }

  @Test
  void fetchStats_withoutSourceFile() {
    //  WHEN
    when(sourceFileRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    var authorityChangeStatDtoCollection = delegate
      .fetchAuthorityLinksStats(FROM_DATE, TO_DATE, DATA_STAT_ACTION, LIMIT_SIZE);

    //  THEN
    assertNotNull(authorityChangeStatDtoCollection);
    assertNotNull(authorityChangeStatDtoCollection.getStats());
    assertEquals(LIMIT_SIZE, authorityChangeStatDtoCollection.getStats().size());

    var resultUserIds = authorityChangeStatDtoCollection.getStats()
      .stream()
      .map(AuthorityStatsDto::getMetadata)
      .map(AuthorityControlMetadata::getStartedByUserId)
      .toList();
    assertNull(authorityChangeStatDtoCollection.getNext());
    assertThat(List.of(USER_ID_1, USER_ID_2)).containsAll(resultUserIds);
  }


  @Test
  void fetchStats_withoutMetadata() {
    //  WHEN
    when(usersClient.query(anyString())).thenReturn(null);

    var authorityChangeStatDtoCollection = delegate
      .fetchAuthorityLinksStats(FROM_DATE, TO_DATE, DATA_STAT_ACTION, LIMIT_SIZE);

    //  THEN
    assertNotNull(authorityChangeStatDtoCollection);
    assertNotNull(authorityChangeStatDtoCollection.getStats());
    assertEquals(LIMIT_SIZE, authorityChangeStatDtoCollection.getStats().size());

    var resultUserIds = authorityChangeStatDtoCollection.getStats()
      .stream()
      .map(AuthorityStatsDto::getMetadata)
      .filter(Objects::nonNull)
      .map(AuthorityControlMetadata::getStartedByUserId)
      .toList();

    assertNull(authorityChangeStatDtoCollection.getNext());
    assertThat(List.of(USER_ID_1, USER_ID_2)).containsAll(resultUserIds);
  }
}
