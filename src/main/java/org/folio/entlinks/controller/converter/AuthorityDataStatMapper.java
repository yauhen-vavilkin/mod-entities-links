package org.folio.entlinks.controller.converter;

import org.folio.entlinks.domain.dto.AuthorityDataStatDto;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthorityDataStatMapper {

  @Mapping(target = "authorityId", source = "source.authorityData.id")
  AuthorityDataStatDto convertToDto(AuthorityDataStat source);

}
