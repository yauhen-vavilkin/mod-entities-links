package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.INPUT_BASE_URL;
import static org.folio.support.base.TestConstants.SOURCE_FILE_CODE;
import static org.folio.support.base.TestConstants.SOURCE_FILE_NAME;
import static org.folio.support.base.TestConstants.SOURCE_FILE_TYPE;
import static org.folio.support.base.TestConstants.TEST_ID;

import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDtoHridManagement;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDto;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.entity.AuthoritySourceFileSource;
import org.folio.spring.testing.type.UnitTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@UnitTest
class AuthoritySourceFileMapperTest {

  public static final String UPDATED_NAME = "Updated Name";
  public static final String UPDATED_TYPE = "Updated Type";
  public static final String UPDATED_CODE = "Updated Code";
  public static final String UPDATED_BASE_URL = "http://updated.base.url/";
  private final AuthoritySourceFileMapper mapper = new AuthoritySourceFileMapperImpl();

  @Test
  void testToEntity() {
    var dto = createAuthoritySourceFileDto();

    var entity = mapper.toEntity(dto);

    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(dto.getId());
    assertThat(entity.getName()).isEqualTo(dto.getName());
    assertThat(entity.getType()).isEqualTo(dto.getType());
    assertThat(entity.getFullBaseUrl()).isEqualTo(StringUtils.appendIfMissing(dto.getBaseUrl(), "/"));
    assertThat(entity.getSource()).isEqualTo(AuthoritySourceFileSource.LOCAL);
    assertThat(entity.getAuthoritySourceFileCodes()).hasSize(1);
    assertThat(entity.getAuthoritySourceFileCodes().iterator().next().getCode()).isEqualTo(dto.getCode());
  }

  @Test
  void testToDto() {
    AuthoritySourceFile sourceFile = createAuthoritySourceFile();

    AuthoritySourceFileDto dto = mapper.toDto(sourceFile);

    assertThat(dto).isNotNull();
    assertThat(sourceFile.getId()).isEqualTo(dto.getId());
    assertThat(sourceFile.getName()).isEqualTo(dto.getName());
    assertThat(sourceFile.getType()).isEqualTo(dto.getType());
    assertThat(sourceFile.getBaseUrl()).isEqualTo(dto.getBaseUrl());
    assertThat(sourceFile.getSource().name()).isEqualTo(dto.getSource().name());
    assertThat(dto.getCodes()).hasSize(sourceFile.getAuthoritySourceFileCodes().size());
  }

  @EnumSource(AuthoritySourceFileSource.class)
  @ParameterizedTest
  void testToDtoSource(AuthoritySourceFileSource source) {
    var dtoSource = mapper.toDtoSource(source);

    assertThat(source.name()).isEqualTo(dtoSource.name());
  }

  @Test
  void testPartialUpdate() {
    var sourceFile = createAuthoritySourceFile();
    var patchDto = new AuthoritySourceFilePatchDto();
    patchDto.setName(UPDATED_NAME);
    patchDto.setType(UPDATED_TYPE);
    patchDto.setCodes(List.of(UPDATED_CODE));
    patchDto.setBaseUrl(UPDATED_BASE_URL);
    patchDto.selectable(true);
    patchDto.hridManagement(new AuthoritySourceFilePatchDtoHridManagement().startNumber(5));

    var updatedFile = mapper.partialUpdate(patchDto, sourceFile);

    assertThat(updatedFile).isNotNull();
    assertThat(updatedFile.getName()).isEqualTo(patchDto.getName());
    assertThat(updatedFile.getType()).isEqualTo(patchDto.getType());
    assertThat(updatedFile.getAuthoritySourceFileCodes().stream().map(AuthoritySourceFileCode::getCode).toList())
        .isEqualTo(patchDto.getCodes());
    assertThat(updatedFile.getFullBaseUrl()).isEqualTo(patchDto.getBaseUrl());
    assertThat(updatedFile.isSelectable()).isEqualTo(patchDto.getSelectable());
    assertThat(updatedFile.getHridStartNumber()).isEqualTo(patchDto.getHridManagement().getStartNumber());
    assertThat(updatedFile.getSource()).isEqualTo(sourceFile.getSource());
    assertThat(updatedFile.getSequenceName()).isEqualTo(sourceFile.getSequenceName());
  }

  @Test
  void testPartialUpdate_noCodesUpdate() {
    var code = new AuthoritySourceFileCode();
    code.setCode("a");
    var codes = Set.of(code);
    var sourceFile = createAuthoritySourceFile();
    sourceFile.setAuthoritySourceFileCodes(codes);

    var patchDto = new AuthoritySourceFilePatchDto();
    patchDto.setName(UPDATED_NAME);
    patchDto.setType(UPDATED_TYPE);

    var updatedFile = mapper.partialUpdate(patchDto, sourceFile);

    assertThat(updatedFile).isNotNull();
    assertThat(updatedFile.getName()).isEqualTo(patchDto.getName());
    assertThat(updatedFile.getType()).isEqualTo(patchDto.getType());
    assertThat(updatedFile.getAuthoritySourceFileCodes()).isEqualTo(codes);
  }

  @Test
  void testToDtoList() {
    var sourceFile = createAuthoritySourceFile();
    var sourceFiles = List.of(sourceFile);

    List<AuthoritySourceFileDto> dtos = mapper.toDtoList(sourceFiles);

    assertThat(dtos).isNotNull();
    assertThat(sourceFiles).hasSize(dtos.size());
    AuthoritySourceFileDto dto1 = dtos.get(0);
    assertThat(sourceFile.getId()).isEqualTo(dto1.getId());
    assertThat(sourceFile.getName()).isEqualTo(dto1.getName());
    assertThat(sourceFile.getType()).isEqualTo(dto1.getType());
    assertThat(sourceFile.getBaseUrl()).isEqualTo(dto1.getBaseUrl());
  }

  @Test
  void testToAuthoritySourceFileCollection() {
    var sourceFilesList = List.of(createAuthoritySourceFile());

    Page<AuthoritySourceFile> sourceFilesPage = new PageImpl<>(sourceFilesList);

    AuthoritySourceFileDtoCollection dtoCollection = mapper.toAuthoritySourceFileCollection(sourceFilesPage);

    List<AuthoritySourceFileDto> dtos = dtoCollection.getAuthoritySourceFiles();
    AuthoritySourceFile sourceFile = sourceFilesList.get(0);
    assertThat(dtoCollection).isNotNull();
    assertThat(sourceFilesList).hasSize(dtoCollection.getTotalRecords());
    assertThat(sourceFile.getId()).isEqualTo(dtos.get(0).getId());
    assertThat(sourceFile.getName()).isEqualTo(dtos.get(0).getName());
    assertThat(sourceFile.getType()).isEqualTo(dtos.get(0).getType());
    assertThat(sourceFile.getBaseUrl()).isEqualTo(dtos.get(0).getBaseUrl());

  }

  @NotNull
  private AuthoritySourceFile createAuthoritySourceFile() {
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setId(TEST_ID);
    sourceFile.setName(SOURCE_FILE_NAME);
    sourceFile.setType(SOURCE_FILE_TYPE);
    sourceFile.setBaseUrl(INPUT_BASE_URL);
    sourceFile.setAuthoritySourceFileCodes(Set.of());
    sourceFile.setSource(AuthoritySourceFileSource.FOLIO);
    sourceFile.setSelectable(false);
    sourceFile.setHridStartNumber(1);
    return sourceFile;
  }

  @NotNull
  private static AuthoritySourceFilePostDto createAuthoritySourceFileDto() {
    var dto = new AuthoritySourceFilePostDto();
    dto.setId(TEST_ID);
    dto.setName(SOURCE_FILE_NAME);
    dto.setType(SOURCE_FILE_TYPE);
    dto.setBaseUrl(INPUT_BASE_URL);
    dto.setCode(SOURCE_FILE_CODE);
    return dto;
  }
}
