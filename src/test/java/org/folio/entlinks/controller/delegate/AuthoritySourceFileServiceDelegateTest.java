package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.INPUT_BASE_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDtoHridManagement;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.folio.spring.test.type.UnitTest;
import org.folio.support.TestDataUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthoritySourceFileServiceDelegateTest {


  private static final String SANITIZED_BASE_URL = "id.loc.gov/authorities/test-source/";

  @Mock
  private AuthoritySourceFileMapper mapper;

  @Mock
  private AuthoritySourceFileService service;

  @InjectMocks
  private AuthoritySourceFileServiceDelegate delegate;

  @Captor
  private ArgumentCaptor<AuthoritySourceFile> sourceFileArgumentCaptor;

  @Test
  void shouldGetSourceFileCollectionByQuery() {
    var expectedCollection = new AuthoritySourceFileDtoCollection();
    when(service.getAll(any(Integer.class), any(Integer.class), any(String.class)))
        .thenReturn(new PageImpl<>(List.of()));
    when(mapper.toAuthoritySourceFileCollection(any(Page.class))).thenReturn(expectedCollection);

    var sourceFiles = delegate.getAuthoritySourceFiles(0, 100, "cql.allRecords=1");

    assertEquals(expectedCollection, sourceFiles);
  }

  @Test
  void shouldGetSourceFileById() {
    var sourceFile = new AuthoritySourceFile();
    var id = UUID.randomUUID();
    var expected = new AuthoritySourceFileDto();
    when(service.getById(id)).thenReturn(sourceFile);
    when(mapper.toDto(sourceFile)).thenReturn(expected);

    var sourceFileDto = delegate.getAuthoritySourceFileById(id);

    assertEquals(expected, sourceFileDto);
  }

  @Test
  void shouldNormalizeBaseUrlForSourceFileCreate() {
    var dto = new AuthoritySourceFilePostDto().baseUrl(INPUT_BASE_URL)
        .hridManagement(new AuthoritySourceFilePostDtoHridManagement().startNumber(10));
    var expected = new AuthoritySourceFile();
    expected.setBaseUrl(INPUT_BASE_URL);
    expected.setSequenceName("sequence_name");
    expected.setHridStartNumber(dto.getHridManagement().getStartNumber());

    when(mapper.toEntity(dto)).thenReturn(expected);
    when(service.create(expected)).thenReturn(expected);
    when(mapper.toDto(expected)).thenReturn(new AuthoritySourceFileDto());

    delegate.createAuthoritySourceFile(dto);

    assertEquals(SANITIZED_BASE_URL, expected.getBaseUrl());
    verify(mapper).toEntity(dto);
    verify(service).create(expected);
    verify(mapper).toDto(expected);
    verify(service).createSequence(expected.getSequenceName(), expected.getHridStartNumber());
    verifyNoMoreInteractions(mapper, service);
  }

  @Test
  void shouldNormalizeBaseUrlForSourceFilePartialUpdate() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setBaseUrl(INPUT_BASE_URL);
    var authority = TestDataUtils.AuthorityTestData.authority(0, 0);
    existing.getAuthorities().add(authority);
    var expected = new AuthoritySourceFile(existing);
    expected.setBaseUrl(SANITIZED_BASE_URL);

    when(service.getById(existing.getId())).thenReturn(existing);
    when(mapper.partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class)))
        .thenAnswer(i -> i.getArguments()[1]);
    when(service.update(any(UUID.class), any(AuthoritySourceFile.class))).thenAnswer(i -> i.getArguments()[1]);
    var dto = new AuthoritySourceFilePatchDto().id(existing.getId()).baseUrl(INPUT_BASE_URL);

    delegate.patchAuthoritySourceFile(existing.getId(), dto);

    verify(service).update(any(UUID.class), sourceFileArgumentCaptor.capture());
    var patchedSourceFile = sourceFileArgumentCaptor.getValue();
    assertThat(expected).usingDefaultComparator().isEqualTo(patchedSourceFile);
    verify(mapper).partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class));
    verify(service).getById(any(UUID.class));
    verifyNoMoreInteractions(mapper, service);
  }
}
