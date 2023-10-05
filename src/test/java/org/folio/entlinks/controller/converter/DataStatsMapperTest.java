package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.entity.AuthorityDataStatAction.DELETE;
import static org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus.ACTUAL;
import static org.folio.entlinks.utils.DateUtils.fromTimestamp;
import static org.folio.support.base.TestConstants.TEST_DATE;
import static org.folio.support.base.TestConstants.TEST_ID;

import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.dto.AuthorityStatsDto;
import org.folio.entlinks.domain.dto.BibStatsDto;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.spring.test.type.UnitTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;


@UnitTest
class DataStatsMapperTest {

  private final DataStatsMapper mapper = new DataStatsMapperImpl();

  @Test
  void testToDtoAuthorityDataStat() {
    AuthorityDataStat source = createAuthorityDataStat();

    AuthorityStatsDto dto = mapper.convertToDto(source);

    assertThat(dto).isNotNull();
    assertThat(dto.getAuthorityId()).isEqualTo(source.getAuthority().getId());
    assertThat(dto.getNaturalIdOld()).isEqualTo(source.getAuthorityNaturalIdOld());
    assertThat(dto.getNaturalIdNew()).isEqualTo(source.getAuthorityNaturalIdNew());
    assertThat(UUID.fromString(dto.getSourceFileOld())).isEqualTo(source.getAuthoritySourceFileOld());
    assertThat(UUID.fromString(dto.getSourceFileNew())).isEqualTo(source.getAuthoritySourceFileNew());
    assertThat(dto.getHeadingOld()).isEqualTo(source.getHeadingOld());
    assertThat(dto.getHeadingNew()).isEqualTo(source.getHeadingNew());
    assertThat(dto.getHeadingTypeOld()).isEqualTo(source.getHeadingTypeOld());
    assertThat(dto.getHeadingTypeNew()).isEqualTo(source.getHeadingTypeNew());
    assertThat(dto.getLbTotal()).isEqualTo(source.getLbTotal());
    assertThat(dto.getLbUpdated()).isEqualTo(source.getLbUpdated());
    assertThat(dto.getLbFailed()).isEqualTo(source.getLbFailed());

  }

  @Test
  void testToDtoInstanceAuthorityLink() {
    InstanceAuthorityLink source = createInstanceAuthorityLink();

    BibStatsDto dto = mapper.convertToDto(source);

    assertThat(source.getAuthority().getNaturalId()).isEqualTo(dto.getAuthorityNaturalId());
    assertThat(source.getLinkingRule().getBibField()).isEqualTo(dto.getBibRecordTag());
    assertThat(source.getInstanceId()).isEqualTo(dto.getInstanceId());
    assertThat(source.getErrorCause()).isEqualTo(dto.getErrorCause());
    assertThat(fromTimestamp(source.getUpdatedAt())).isEqualTo(dto.getUpdatedAt());
  }

  @Test
  void testConvertToDtoList_InstanceAuthorityLink() {
    var sourceList = List.of(createInstanceAuthorityLink(), createInstanceAuthorityLink());

    List<BibStatsDto> dtoList = mapper.convertToDto(sourceList);
    assertThat(sourceList).hasSize(dtoList.size());

    InstanceAuthorityLink source = sourceList.get(0);
    BibStatsDto dto = dtoList.get(0);
    assertThat(source.getAuthority().getNaturalId()).isEqualTo(dto.getAuthorityNaturalId());
    assertThat(source.getLinkingRule().getBibField()).isEqualTo(dto.getBibRecordTag());
    assertThat(source.getInstanceId()).isEqualTo(dto.getInstanceId());
    assertThat(source.getErrorCause()).isEqualTo(dto.getErrorCause());
    assertThat(fromTimestamp(source.getUpdatedAt())).isEqualTo(dto.getUpdatedAt());
  }

  @NotNull
  private static InstanceAuthorityLink createInstanceAuthorityLink() {
    InstanceAuthorityLink source = new InstanceAuthorityLink();
    source.setAuthority(new Authority());
    source.setInstanceId(TEST_ID);
    source.setUpdatedAt(TEST_DATE);
    source.setErrorCause("SomeErrorCause");
    source.setLinkingRule(new InstanceAuthorityLinkingRule());
    source.setStatus(ACTUAL);
    return source;
  }

  @NotNull
  private static AuthorityDataStat createAuthorityDataStat() {
    AuthorityDataStat source = new AuthorityDataStat();
    source.setAuthority(new Authority().withId(TEST_ID));
    source.setAuthorityNaturalIdOld("OldNaturalId");
    source.setAuthorityNaturalIdNew("NewNaturalId");
    source.setAuthoritySourceFileOld(TEST_ID);
    source.setAuthoritySourceFileNew(TEST_ID);
    source.setId(TEST_ID);
    source.setAction(DELETE);
    source.setHeadingOld("OldHeading");
    source.setHeadingNew("NewHeading");
    source.setHeadingTypeOld("OldHeadingType");
    source.setHeadingTypeNew("NewHeadingType");
    source.setLbTotal(10);
    source.setLbUpdated(5);
    source.setLbFailed(2);
    return source;
  }
}
