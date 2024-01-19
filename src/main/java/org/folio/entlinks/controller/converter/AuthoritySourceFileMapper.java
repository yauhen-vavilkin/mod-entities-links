package org.folio.entlinks.controller.converter;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDto;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.entity.AuthoritySourceFileSource;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.utils.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.AfterMapping;
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
  @Mapping(target = "source", expression = "java(org.folio.entlinks.domain.entity.AuthoritySourceFileSource.LOCAL)")
  AuthoritySourceFile toEntity(AuthoritySourceFilePostDto authoritySourceFilePostDto);

  @Mapping(target = "updatedDate", ignore = true)
  @Mapping(target = "updatedByUserId", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "createdByUserId", ignore = true)
  @Mapping(target = "sequenceName", ignore = true)
  @Mapping(target = "authoritySourceFileCodes", expression = "java(toEntityCodes(authoritySourceFileDto.getCodes()))")
  @Mapping(target = "source", expression = "java(org.folio.entlinks.domain.entity.AuthoritySourceFileSource.FOLIO)")
  AuthoritySourceFile toEntity(AuthoritySourceFileDto authoritySourceFileDto);

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
           expression = "java(toEntityCodes(authoritySourceFileDto, authoritySourceFile))")
  @Mapping(target = "hridStartNumber", source = "hridManagement.startNumber")
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  AuthoritySourceFile partialUpdate(AuthoritySourceFilePatchDto authoritySourceFileDto,
                                    @MappingTarget AuthoritySourceFile authoritySourceFile);

  List<AuthoritySourceFileDto> toDtoList(Iterable<AuthoritySourceFile> authoritySourceFileIterable);

  default AuthoritySourceFileDto.SourceEnum toDtoSource(AuthoritySourceFileSource source) {
    return AuthoritySourceFileDto.SourceEnum.valueOf(source.name());
  }

  @AfterMapping
  default void processUrl(AuthoritySourceFilePatchDto source, @MappingTarget AuthoritySourceFile target) {
    setUrlProperties(source.getBaseUrl(), target);
  }

  @AfterMapping
  default void processUrl(AuthoritySourceFilePostDto source, @MappingTarget AuthoritySourceFile target) {
    setUrlProperties(source.getBaseUrl(), target);
  }

  @AfterMapping
  default void processUrl(AuthoritySourceFileDto source, @MappingTarget AuthoritySourceFile target) {
    setUrlProperties(source.getBaseUrl(), target);
  }

  @AfterMapping
  default void processUrl(AuthoritySourceFile source, @MappingTarget AuthoritySourceFileDto target) {
    if (StringUtils.isBlank(source.getBaseUrl())) {
      return;
    }

    target.setBaseUrl(source.getFullBaseUrl());
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

  default Set<AuthoritySourceFileCode> toEntityCodes(AuthoritySourceFilePatchDto authoritySourceFileDto,
                                                     AuthoritySourceFile authoritySourceFile) {
    var dtoCodes = authoritySourceFileDto.getCodes();
    if (dtoCodes == null) {
      return authoritySourceFile.getAuthoritySourceFileCodes();
    }

    return toEntityCodes(dtoCodes);
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

  @NotNull
  private static String getHostPath(URL url) {
    return StringUtils.appendIfMissing(url.getHost() + url.getPath(), "/");
  }

  private static void setUrlProperties(String baseUrlDto, AuthoritySourceFile target) {
    if (StringUtils.isBlank(baseUrlDto)) {
      return;
    }

    var url = getUrl(baseUrlDto);
    target.setBaseUrlProtocol(url.getProtocol());
    target.setBaseUrl(getHostPath(url));
  }

  @NotNull
  private static URL getUrl(String baseUrl) {
    try {
      return URI.create(baseUrl).toURL();
    } catch (MalformedURLException e) {
      throw new RequestBodyValidationException(e.getMessage(), Collections.emptyList());
    }
  }
}
