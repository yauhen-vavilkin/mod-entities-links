package org.folio.entlinks.controller.converter;

import org.folio.entlinks.domain.dto.AuthorityRecord;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DataMapper {
  AuthorityData convertToData(AuthorityRecord authority);
}
