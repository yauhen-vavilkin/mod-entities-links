package org.folio.entlinks.controller.converter;

import org.folio.entlinks.domain.dto.ReindexJobDto;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReindexJobMapper {

  ReindexJobDto toDto(ReindexJob reindexJob);

}
