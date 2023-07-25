package org.folio.entlinks.controller.converter;

import java.util.List;
import org.folio.entlinks.domain.dto.ReindexJobDto;
import org.folio.entlinks.domain.dto.ReindexJobDtoCollection;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.data.domain.Page;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReindexJobMapper {

  ReindexJobDto toDto(ReindexJob reindexJob);

  List<ReindexJobDto> toDtoList(Iterable<ReindexJob> authorityStorageIterable);

  default ReindexJobDtoCollection toReindexJobCollection(
      Page<ReindexJob> authorityStorageIterable) {
    var reindexJobDtos = toDtoList(authorityStorageIterable.getContent());
    return new ReindexJobDtoCollection(reindexJobDtos, (int) authorityStorageIterable.getTotalElements());
  }

}
