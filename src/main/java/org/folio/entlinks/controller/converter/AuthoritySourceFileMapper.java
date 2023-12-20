package org.folio.entlinks.controller.converter;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDto;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.utils.DateUtils;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthoritySourceFileMapper {

  @Mapping(target = "updatedDate", ignore = true)
  @Mapping(target = "updatedByUserId", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "createdByUserId", ignore = true)
  @Mapping(target = "sequenceName", ignore = true)
  @Mapping(target = "hridStartNumber", source = "hridManagement.startNumber")
  @Mapping(target = "authoritySourceFileCodes",
      expression = "java(toEntityCodes(List.of(authoritySourceFilePostDto.getCode())))")
  @Mapping(target = "source", constant = "local")
  AuthoritySourceFile toEntity(AuthoritySourceFilePostDto authoritySourceFilePostDto);

  @Mapping(target = "codes",
           expression = "java(toDtoCodes(authoritySourceFile.getAuthoritySourceFileCodes()))")
  @Mapping(target = "source", expression = "java(toDtoSource(authoritySourceFile.getSource()))")
  @Mapping(target = "metadata.updatedDate", source = "updatedDate")
  @Mapping(target = "metadata.updatedByUserId", source = "updatedByUserId")
  @Mapping(target = "metadata.createdDate", source = "createdDate")
  @Mapping(target = "metadata.createdByUserId", source = "createdByUserId")
  @Mapping(target = "hridManagement.startNumber", source = "hridStartNumber")
  AuthoritySourceFileDto toDto(AuthoritySourceFile authoritySourceFile);

  @Mapping(target = "authoritySourceFileCodes",
           expression = "java(toEntityCodes(authoritySourceFileDto.getCodes()))")
  @Mapping(target = "source", expression = "java(toSource(authoritySourceFileDto.getSource()))")
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  AuthoritySourceFile partialUpdate(AuthoritySourceFilePatchDto authoritySourceFileDto,
                                    @MappingTarget AuthoritySourceFile authoritySourceFile);

  List<AuthoritySourceFileDto> toDtoList(Iterable<AuthoritySourceFile> authoritySourceFileIterable);

  default AuthoritySourceFileDto.SourceEnum toDtoSource(String source) {
    return AuthoritySourceFileDto.SourceEnum.fromValue(source);
  }

  default String toSource(AuthoritySourceFilePatchDto.SourceEnum dtoSource) {
    if (dtoSource == null) {
      return null;
    }

    return dtoSource.getValue();
  }

  default AuthoritySourceFileDtoCollection toAuthoritySourceFileCollection(
    Page<AuthoritySourceFile> authoritySourceFiles) {
    var sourceFileDtos = toDtoList(authoritySourceFiles);
    return new AuthoritySourceFileDtoCollection((int) authoritySourceFiles.getTotalElements())
        .authoritySourceFiles(sourceFileDtos);
  }

  default Set<AuthoritySourceFileCode> toEntityCodes(List<String> codes) {
    return codes.stream()
      .map(this::toEntityCode)
      .collect(Collectors.toSet());
  }

  default AuthoritySourceFileCode toEntityCode(String code) {
    var authoritySourceFileCode = new AuthoritySourceFileCode();
    authoritySourceFileCode.setCode(code);
    return authoritySourceFileCode;
  }

  default List<String> toDtoCodes(Set<AuthoritySourceFileCode> codes) {
    return codes.stream()
      .map(AuthoritySourceFileCode::getCode)
      .toList();
  }

  default OffsetDateTime map(Timestamp timestamp) {
    return DateUtils.fromTimestamp(timestamp);
  }
}
