package org.folio.entlinks.controller.converter;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityDtoCollection;
import org.folio.entlinks.domain.dto.AuthorityDtoIdentifier;
import org.folio.entlinks.domain.dto.AuthorityDtoNote;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityIdentifier;
import org.folio.entlinks.domain.entity.AuthorityNote;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.utils.DateUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthorityMapper {

  @Mapping(target = "updatedDate", ignore = true)
  @Mapping(target = "updatedByUserId", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "createdByUserId", ignore = true)
  @Mapping(target = "authoritySourceFile", expression = "java(toAuthoritySourceFile(dto))")
  @Mapping(target = "subjectHeadingCode", expression = "java(toSubjectHeadingCode(dto.getSubjectHeadings()))")
  Authority toEntity(AuthorityDto dto);

  @Mapping(target = "metadata.updatedDate", source = "updatedDate")
  @Mapping(target = "metadata.updatedByUserId", source = "updatedByUserId")
  @Mapping(target = "metadata.createdDate", source = "createdDate")
  @Mapping(target = "metadata.createdByUserId", source = "createdByUserId")
  @Mapping(target = "sourceFileId", source = "authoritySourceFile.id")
  @Mapping(target = "subjectHeadings",
      expression = "java(toSubjectHeadingsDto(authority.getSubjectHeadingCode()))")
  AuthorityDto toDto(Authority authority);

  AuthorityIdentifier toAuthorityIdentifier(AuthorityDtoIdentifier dto);

  AuthorityDtoIdentifier toAuthorityDtoIdentifier(AuthorityIdentifier identifier);

  AuthorityNote toAuthorityNote(AuthorityDtoNote dto);

  AuthorityDtoNote toAuthorityDtoNote(AuthorityNote note);

  List<AuthorityDto> toDtoList(Iterable<Authority> authorityStorageIterable);

  default AuthorityDtoCollection toAuthorityCollection(
      Page<Authority> authorityStorageIterable) {
    var authorityDtos = toDtoList(authorityStorageIterable.getContent());
    return new AuthorityDtoCollection(authorityDtos, (int) authorityStorageIterable.getTotalElements());
  }

  default AuthoritySourceFile toAuthoritySourceFile(AuthorityDto dto) {
    if (dto == null || dto.getSourceFileId() == null) {
      return null;
    }

    var sourceFile = new AuthoritySourceFile();
    sourceFile.setId(dto.getSourceFileId());

    return sourceFile;
  }

  default Character toSubjectHeadingCode(String subjectHeadings) {
    if (subjectHeadings == null) {
      return null;
    }

    return subjectHeadings.charAt(0);
  }

  default String toSubjectHeadingsDto(Character subjectHeadingCode) {
    if (subjectHeadingCode == null) {
      return null;
    }

    return String.valueOf(subjectHeadingCode);
  }

  @AfterMapping
  default void authorityPostProcess(AuthorityDto source, @MappingTarget Authority target) {
    AuthorityStorageUtilityMapper.extractAuthorityHeading(source, target);
    AuthorityStorageUtilityMapper.extractAuthoritySftHeadings(source, target);
    AuthorityStorageUtilityMapper.extractAuthoritySaftHeadings(source, target);
  }

  @AfterMapping
  default void authorityDtoPostProcessing(Authority source, @MappingTarget AuthorityDto target) {
    AuthorityStorageUtilityMapper.extractAuthorityDtoHeadingValue(source, target);
    AuthorityStorageUtilityMapper.extractAuthorityDtoSftHeadings(source, target);
    AuthorityStorageUtilityMapper.extractAuthorityDtoSaftHeadings(source, target);
  }

  default OffsetDateTime map(Timestamp timestamp) {
    return DateUtils.fromTimestamp(timestamp);
  }
}
