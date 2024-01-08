package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus.ACTUAL;
import static org.folio.support.base.TestConstants.TEST_ID;
import static org.folio.support.base.TestConstants.TEST_PROPERTY_VALUE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.folio.entlinks.domain.dto.InstanceLinkDto;
import org.folio.entlinks.domain.dto.LinksCountDto;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.spring.testing.type.UnitTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

@UnitTest
class InstanceAuthorityLinkMapperTest {

  private static final Integer TEST_INTEGER_ID = ThreadLocalRandom.current().nextInt();
  private static final Long TEST_LONG_ID = 789L;
  private final InstanceAuthorityLinkMapperImpl mapper = new InstanceAuthorityLinkMapperImpl();


  @Test
  void testConvertToDto_InstanceAuthorityLink() {
    InstanceAuthorityLink source = createInstanceAuthorityLink();

    InstanceLinkDto dto = mapper.convertToDto(source);

    assertThat(source.getAuthority().getId()).isEqualTo(dto.getAuthorityId());
    assertThat(source.getAuthority().getNaturalId()).isEqualTo(dto.getAuthorityNaturalId());
    assertThat(source.getLinkingRule().getId()).isEqualTo(dto.getLinkingRuleId());
    assertThat(source.getId().intValue()).isEqualTo(dto.getId());
    assertThat(source.getInstanceId()).isEqualTo(dto.getInstanceId());
    assertThat(source.getStatus().name()).isEqualTo(dto.getStatus());
    assertThat(source.getErrorCause()).isEqualTo(dto.getErrorCause());

  }

  @Test
  void testConvertDto_InstanceLinkDto() {
    InstanceLinkDto source = createInstanceLinkDto();

    InstanceAuthorityLink link = mapper.convertDto(source);

    assertThat(link.getAuthority().getId()).isEqualTo(source.getAuthorityId());
    assertThat(link.getAuthority().getNaturalId()).isEqualTo(source.getAuthorityNaturalId());
    assertThat(link.getLinkingRule().getId()).isEqualTo(source.getLinkingRuleId());
    assertThat(link.getId().intValue()).isEqualTo(source.getId());
    assertThat(link.getInstanceId()).isEqualTo(source.getInstanceId());
    assertThat(link.getStatus().name()).isEqualTo(source.getStatus());

  }

  @Test
  void testConvertDtoList_InstanceLinkDtoList() {
    List<InstanceLinkDto> sourceList = List.of(createInstanceLinkDto());

    List<InstanceAuthorityLink> linkList = mapper.convertDto(sourceList);

    assertThat(sourceList).hasSize(linkList.size());
    assertThat(sourceList.get(0).getAuthorityId()).isEqualTo(linkList.get(0).getAuthority().getId());
    assertThat(sourceList.get(0).getAuthorityNaturalId()).isEqualTo(linkList.get(0).getAuthority().getNaturalId());
    assertThat(sourceList.get(0).getLinkingRuleId()).isEqualTo(linkList.get(0).getLinkingRule().getId());
    assertThat(sourceList.get(0).getId()).isEqualTo(linkList.get(0).getId().intValue());
    assertThat(sourceList.get(0).getInstanceId()).isEqualTo(linkList.get(0).getInstanceId());
    assertThat(sourceList.get(0).getStatus()).isEqualTo(linkList.get(0).getStatus().name());
  }

  @Test
  void testConvert_Map() {
    Map<UUID, Integer> sourceMap = new HashMap<>();
    UUID uuidKey = UUID.fromString("38d3a441-c100-5e8d-bd12-71bde492b723");
    Integer value = 1;
    sourceMap.put(uuidKey, value);

    List<LinksCountDto> dtoList = mapper.convert(sourceMap);
    assertThat(dtoList).hasSize(1);
    assertThat(uuidKey).isEqualTo(dtoList.get(0).getId());
    assertThat(value).isEqualTo(dtoList.get(0).getTotalLinks());
  }

  @NotNull
  private static InstanceLinkDto createInstanceLinkDto() {
    InstanceLinkDto source = new InstanceLinkDto();
    source.setAuthorityId(TEST_ID);
    source.setAuthorityNaturalId(TEST_PROPERTY_VALUE);
    source.setLinkingRuleId(TEST_INTEGER_ID);
    source.setId(TEST_INTEGER_ID);
    source.setInstanceId(TEST_ID);
    source.setStatus(ACTUAL.name());
    return source;
  }

  @NotNull
  private static InstanceAuthorityLink createInstanceAuthorityLink() {
    InstanceAuthorityLink source = new InstanceAuthorityLink();
    source.setAuthority(new Authority());
    source.setLinkingRule(new InstanceAuthorityLinkingRule());
    source.setId(TEST_LONG_ID);
    source.setInstanceId(TEST_ID);
    source.setStatus(ACTUAL);
    source.setErrorCause(TEST_PROPERTY_VALUE);
    return source;
  }
}
