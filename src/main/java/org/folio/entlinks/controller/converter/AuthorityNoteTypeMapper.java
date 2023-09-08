package org.folio.entlinks.controller.converter;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import org.folio.entlinks.domain.dto.AuthorityNoteTypeDto;
import org.folio.entlinks.domain.dto.AuthorityNoteTypeDtoCollection;
import org.folio.entlinks.domain.entity.AuthorityNoteType;
import org.folio.entlinks.utils.DateUtils;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthorityNoteTypeMapper {

  @Mapping(target = "updatedDate", ignore = true)
  @Mapping(target = "updatedByUserId", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "createdByUserId", ignore = true)
  AuthorityNoteType toEntity(AuthorityNoteTypeDto authorityNoteTypeDto);

  @Mapping(target = "metadata.updatedDate", source = "updatedDate")
  @Mapping(target = "metadata.updatedByUserId", source = "updatedByUserId")
  @Mapping(target = "metadata.createdDate", source = "createdDate")
  @Mapping(target = "metadata.createdByUserId", source = "createdByUserId")
  AuthorityNoteTypeDto toDto(AuthorityNoteType authorityNoteType);

  @Mapping(target = "updatedDate", ignore = true)
  @Mapping(target = "updatedByUserId", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "createdByUserId", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  AuthorityNoteType partialUpdate(AuthorityNoteTypeDto authorityNoteTypeDto,
                                  @MappingTarget AuthorityNoteType authorityNoteType);

  List<AuthorityNoteTypeDto> toDtoList(Iterable<AuthorityNoteType> authorityNoteTypeIterable);

  default AuthorityNoteTypeDtoCollection toAuthorityNoteTypeCollection(
    Page<AuthorityNoteType> authorityNoteTypes) {
    var noteTypes = toDtoList(authorityNoteTypes);
    return new AuthorityNoteTypeDtoCollection(noteTypes, (int) authorityNoteTypes.getTotalElements());
  }

  default OffsetDateTime map(Timestamp timestamp) {
    return DateUtils.fromTimestamp(timestamp);
  }
}
