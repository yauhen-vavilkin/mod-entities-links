package org.folio.entlinks.controller.delegate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthoritySourceFileServiceDelegateTest {

  private static final String INPUT_BASE_URL = "https://www.id.loc.gov/authorities/test-source";

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
  void shouldNormalizeBaseUrlForSourceFileCreate() {
    var dto = new AuthoritySourceFileDto().baseUrl(INPUT_BASE_URL);
    var expected = new AuthoritySourceFile();
    expected.setBaseUrl(INPUT_BASE_URL);

    when(mapper.toEntity(dto)).thenReturn(expected);
    when(service.create(expected)).thenReturn(expected);
    when(mapper.toDto(expected)).thenReturn(dto);

    delegate.createAuthoritySourceFile(dto);

    assertEquals(SANITIZED_BASE_URL, expected.getBaseUrl());
    verify(mapper).toEntity(dto);
    verify(service).create(expected);
    verify(mapper).toDto(expected);
    verifyNoMoreInteractions(mapper, service);
  }

  @Test
  void shouldNormalizeBaseUrlForSourceFileUpdate() {
    var id = UUID.randomUUID();
    var dto = new AuthoritySourceFileDto().id(id).baseUrl(INPUT_BASE_URL);
    var expected = new AuthoritySourceFile();
    expected.setBaseUrl(INPUT_BASE_URL);

    when(mapper.toEntity(dto)).thenReturn(expected);
    when(service.update(id, expected)).thenReturn(expected);

    delegate.updateAuthoritySourceFile(id, dto);

    assertEquals(SANITIZED_BASE_URL, expected.getBaseUrl());
    verify(mapper).toEntity(dto);
    verify(service).update(id, expected);
    verifyNoMoreInteractions(mapper, service);
  }

  @Test
  void shouldNormalizeBaseUrlForSourceFilePartialUpdate() {
    var id = UUID.randomUUID();
    var existing = new AuthoritySourceFile();
    existing.setBaseUrl(INPUT_BASE_URL);

    when(service.getById(id)).thenReturn(existing);
    when(mapper.partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class)))
        .thenAnswer(i -> i.getArguments()[1]);
    when(service.update(any(UUID.class), any(AuthoritySourceFile.class))).thenAnswer(i -> i.getArguments()[1]);
    var dto = new AuthoritySourceFilePatchDto().id(id).baseUrl(INPUT_BASE_URL);

    delegate.patchAuthoritySourceFile(id, dto);

    verify(service).update(any(UUID.class), sourceFileArgumentCaptor.capture());
    var partchedSourceFile = sourceFileArgumentCaptor.getValue();
    assertEquals(SANITIZED_BASE_URL, partchedSourceFile.getBaseUrl());
    verify(mapper).partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class));
    verify(service).getById(any(UUID.class));
    verifyNoMoreInteractions(mapper, service);
  }
}
