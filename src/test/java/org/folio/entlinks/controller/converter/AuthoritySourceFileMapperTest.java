package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.dto.AuthoritySourceFileDto.SourceEnum.FOLIO;
import static org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto.SourceEnum.LOCAL;
import static org.folio.support.base.TestConstants.INPUT_BASE_URL;
import static org.folio.support.base.TestConstants.SOURCE_FILE_CODE;
import static org.folio.support.base.TestConstants.SOURCE_FILE_NAME;
import static org.folio.support.base.TestConstants.SOURCE_FILE_TYPE;
import static org.folio.support.base.TestConstants.TEST_ID;

import java.util.List;
import java.util.Set;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDto;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.spring.test.type.UnitTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@UnitTest
class AuthoritySourceFileMapperTest {

  public static final String UPDATED_NAME = "Updated Name";
  public static final String UPDATED_TYPE = "Updated Type";
  public static final String UPDATED_CODE = "Updated Code";
  public static final String UPDATED_BASE_URL = "Updated Base Url";
  private final AuthoritySourceFileMapper mapper = new AuthoritySourceFileMapperImpl();

  @Test
  void testToEntity() {
    var dto = createAuthoritySourceFileDto();

    var entity = mapper.toEntity(dto);

    assertThat(entity).isNotNull();
    assertThat(dto.getId()).isEqualTo(entity.getId());
    assertThat(dto.getName()).isEqualTo(entity.getName());
    assertThat(dto.getType()).isEqualTo(entity.getType());
    assertThat(dto.getBaseUrl()).isEqualTo(entity.getBaseUrl());
    assertThat(entity.getSource()).isEqualTo("local");
    assertThat(entity.getAuthoritySourceFileCodes()).hasSize(1);
    assertThat(dto.getCode()).isEqualTo(entity.getAuthoritySourceFileCodes().iterator().next().getCode());
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
    assertThat(sourceFile.getSource()).isEqualTo(dto.getSource().getValue());
    assertThat(dto.getCodes()).hasSize(sourceFile.getAuthoritySourceFileCodes().size());
  }

  @Test
  void testPartialUpdate() {
    AuthoritySourceFile sourceFile = createAuthoritySourceFile();

    AuthoritySourceFilePatchDto patchDto = new AuthoritySourceFilePatchDto();
    patchDto.setId(TEST_ID);
    patchDto.setName(UPDATED_NAME);
    patchDto.setType(UPDATED_TYPE);
    patchDto.setCodes(List.of(UPDATED_CODE));
    patchDto.setBaseUrl(UPDATED_BASE_URL);
    patchDto.setSource(LOCAL);

    AuthoritySourceFile updatedFile = mapper.partialUpdate(patchDto, sourceFile);

    assertThat(updatedFile).isNotNull();
    assertThat(patchDto.getId()).isEqualTo(updatedFile.getId());
    assertThat(patchDto.getName()).isEqualTo(updatedFile.getName());
    assertThat(patchDto.getType()).isEqualTo(updatedFile.getType());
    assertThat(patchDto.getBaseUrl()).isEqualTo(updatedFile.getBaseUrl());
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
    sourceFile.setSource(FOLIO.getValue());
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
