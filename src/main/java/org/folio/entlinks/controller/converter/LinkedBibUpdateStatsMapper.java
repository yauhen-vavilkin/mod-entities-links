package org.folio.entlinks.controller.converter;

import static org.folio.entlinks.utils.DateUtils.fromTimestamp;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import org.folio.entlinks.domain.dto.BibStatsDto;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LinkedBibUpdateStatsMapper {

  @Mapping(target = "authorityNaturalId", source = "authorityData.naturalId")
  BibStatsDto convertToDto(InstanceAuthorityLink source);

  default List<BibStatsDto> convertToDto(List<InstanceAuthorityLink> source) {
    return source.stream()
      .map(this::convertToDto)
      .toList();
  }

  default OffsetDateTime map(Timestamp timestamp) {
    return fromTimestamp(timestamp);
  }

}
