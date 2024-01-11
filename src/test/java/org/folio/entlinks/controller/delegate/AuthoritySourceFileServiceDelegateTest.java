package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.CREATE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.DELETE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.UPDATE;
import static org.folio.support.base.TestConstants.CENTRAL_TENANT_ID;
import static org.folio.support.base.TestConstants.INPUT_BASE_URL;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.dto.AuthoritySourceFileHridDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDtoHridManagement;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDtoHridManagement;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileSource;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.folio.entlinks.service.consortium.UserTenantsService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthoritySourceFilePropagationService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.folio.support.TestDataUtils;
import org.folio.tenant.domain.dto.Parameter;
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
  @Mock
  private UserTenantsService tenantsService;
  @Mock
  private ConsortiumAuthoritySourceFilePropagationService propagationService;
  @Mock
  private FolioExecutionContext context;

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
    when(context.getTenantId()).thenReturn(TENANT_ID);

    delegate.createAuthoritySourceFile(dto);

    assertEquals(SANITIZED_BASE_URL, expected.getBaseUrl());
    verify(mapper).toEntity(dto);
    verify(service).create(expected);
    verify(mapper).toDto(expected);
    verify(service).createSequence(expected.getSequenceName(), expected.getHridStartNumber());
    verify(propagationService).propagate(expected, CREATE, TENANT_ID);
    verifyNoMoreInteractions(mapper, service);
  }

  @Test
  void shouldNormalizeBaseUrlForSourceFileCreateOnConsortiumCentralTenant() {
    var dto = new AuthoritySourceFilePostDto().baseUrl(INPUT_BASE_URL)
        .hridManagement(new AuthoritySourceFilePostDtoHridManagement().startNumber(10));
    var expected = new AuthoritySourceFile();
    expected.setBaseUrl(INPUT_BASE_URL);
    expected.setSequenceName("sequence_name");
    expected.setHridStartNumber(dto.getHridManagement().getStartNumber());

    when(context.getTenantId()).thenReturn(CENTRAL_TENANT_ID);
    when(tenantsService.getCentralTenant(CENTRAL_TENANT_ID)).thenReturn(Optional.of(CENTRAL_TENANT_ID));
    when(mapper.toEntity(dto)).thenReturn(expected);
    when(service.create(expected)).thenReturn(expected);
    when(mapper.toDto(expected)).thenReturn(new AuthoritySourceFileDto());

    delegate.createAuthoritySourceFile(dto);

    assertEquals(SANITIZED_BASE_URL, expected.getBaseUrl());
    verify(mapper).toEntity(dto);
    verify(service).create(expected);
    verify(mapper).toDto(expected);
    verify(service).createSequence(expected.getSequenceName(), expected.getHridStartNumber());
    verify(propagationService).propagate(expected, CREATE, CENTRAL_TENANT_ID);
    verifyNoMoreInteractions(mapper, service);
  }

  @Test
  void shouldNormalizeBaseUrlForSourceFilePartialUpdate() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setBaseUrl(INPUT_BASE_URL);
    existing.setSource(AuthoritySourceFileSource.FOLIO);
    var authority = TestDataUtils.AuthorityTestData.authority(0, 0);
    existing.getAuthorities().add(authority);
    var expected = new AuthoritySourceFile(existing);
    expected.setBaseUrl(SANITIZED_BASE_URL);

    when(service.getById(existing.getId())).thenReturn(existing);
    when(service.authoritiesExistForSourceFile(existing.getId())).thenReturn(true);
    when(mapper.partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class)))
        .thenAnswer(i -> i.getArguments()[1]);
    when(service.update(any(UUID.class), any(AuthoritySourceFile.class))).thenAnswer(i -> i.getArguments()[1]);
    when(context.getTenantId()).thenReturn(TENANT_ID);
    var dto = new AuthoritySourceFilePatchDto().baseUrl(INPUT_BASE_URL);

    delegate.patchAuthoritySourceFile(existing.getId(), dto);

    verify(service).update(eq(existing.getId()), sourceFileArgumentCaptor.capture());
    var patchedSourceFile = sourceFileArgumentCaptor.getValue();
    assertThat(expected).usingDefaultComparator().isEqualTo(patchedSourceFile);
    verify(service).authoritiesExistForSourceFile(existing.getId());
    verify(mapper).partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class));
    verify(service).getById(any(UUID.class));
    verify(propagationService).propagate(expected, UPDATE, TENANT_ID);
    verifyNoMoreInteractions(mapper, service);
  }

  @Test
  void shouldNotPatchAuthoritySourceFile_whenSourceFolioOrAuthoritiesReferenced() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setSource(AuthoritySourceFileSource.FOLIO);
    var dto = new AuthoritySourceFilePatchDto()
        .codes(List.of("a", "b"))
        .hridManagement(new AuthoritySourceFilePatchDtoHridManagement().startNumber(1));
    var expected = new RequestBodyValidationException(
        "Unable to patch. Authority source file source is FOLIO or it has authority references",
        List.of(new Parameter("codes").value("a,b"), new Parameter("hridManagement.startNumber").value("1")));

    var id = existing.getId();
    when(service.getById(id)).thenReturn(existing);
    when(service.authoritiesExistForSourceFile(id)).thenReturn(true);

    var ex = assertThrows(RequestBodyValidationException.class,
        () -> delegate.patchAuthoritySourceFile(id, dto));

    assertThat(ex.getMessage()).isEqualTo(expected.getMessage());
    assertThat(ex.getInvalidParameters()).isEqualTo(expected.getInvalidParameters());
    verifyNoInteractions(mapper, propagationService);
    verifyNoMoreInteractions(service);
  }

  @Test
  void shouldDeleteAuthoritySourceFileById() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);

    when(service.getById(existing.getId())).thenReturn(existing);
    when(context.getTenantId()).thenReturn(TENANT_ID);

    delegate.deleteAuthoritySourceFileById(existing.getId());

    verify(service).deleteById(existing.getId());
    verify(propagationService).propagate(existing, DELETE, TENANT_ID);
  }

  @Test
  void shouldNotCreateForConsortiumMemberTenant() {
    var dto = new AuthoritySourceFilePostDto();

    when(context.getTenantId()).thenReturn(TENANT_ID);
    when(tenantsService.getCentralTenant(TENANT_ID)).thenReturn(Optional.of(CENTRAL_TENANT_ID));

    var exc = assertThrows(RequestBodyValidationException.class, () -> delegate.createAuthoritySourceFile(dto));

    assertThat(exc.getMessage()).isEqualTo("Action 'create' is not supported for consortium member tenant");
    assertThat(exc.getInvalidParameters()).hasSize(1);
    assertThat(exc.getInvalidParameters().get(0))
        .matches(param -> param.getKey().equals("tenantId") && param.getValue().equals(TENANT_ID));
    verifyNoInteractions(mapper);
    verifyNoInteractions(service);
  }

  @Test
  void shouldGetNextHrid() {
    var id = UUID.randomUUID();
    var code = "CODE10";
    when(context.getTenantId()).thenReturn(TENANT_ID);
    when(tenantsService.getCentralTenant(TENANT_ID)).thenReturn(Optional.empty());
    when(service.nextHrid(id)).thenReturn(code);

    var hridDto = delegate.getAuthoritySourceFileNextHrid(id);

    assertThat(hridDto)
      .extracting(AuthoritySourceFileHridDto::getId, AuthoritySourceFileHridDto::getHrid)
      .containsExactly(id, code);
    verify(service).nextHrid(id);
  }

  @Test
  void shouldNotNextHridForConsortiumMemberTenant() {
    when(context.getTenantId()).thenReturn(TENANT_ID);
    when(tenantsService.getCentralTenant(TENANT_ID)).thenReturn(Optional.of(CENTRAL_TENANT_ID));

    var id = UUID.randomUUID();
    var exc = assertThrows(RequestBodyValidationException.class, () -> delegate.getAuthoritySourceFileNextHrid(id));

    assertThat(exc.getMessage()).isEqualTo("Action 'next HRID' is not supported for consortium member tenant");
    assertThat(exc.getInvalidParameters()).hasSize(1);
    assertThat(exc.getInvalidParameters().get(0))
      .matches(param -> param.getKey().equals("tenantId") && param.getValue().equals(TENANT_ID));
    verifyNoInteractions(service);
  }

  @Test
  void shouldNotUpdateConsortiumShadowCopy() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setSource(AuthoritySourceFileSource.CONSORTIUM);
    var id = existing.getId();
    var dto = new AuthoritySourceFilePatchDto();

    when(service.getById(existing.getId())).thenReturn(existing);

    var exc = assertThrows(RequestBodyValidationException.class,
        () -> delegate.patchAuthoritySourceFile(id, dto));

    assertThat(exc.getMessage()).isEqualTo("UPDATE is not applicable to consortium shadow copy");
    assertThat(exc.getInvalidParameters()).hasSize(1);
    assertThat(exc.getInvalidParameters().get(0))
        .matches(param -> param.getKey().equals("id") && param.getValue().equals(String.valueOf(id)));
    verifyNoInteractions(mapper);
    verifyNoMoreInteractions(service);
    verifyNoInteractions(propagationService);
  }

  @Test
  void shouldNotDeleteConsortiumShadowCopy() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setSource(AuthoritySourceFileSource.CONSORTIUM);
    var id = existing.getId();

    when(service.getById(existing.getId())).thenReturn(existing);

    var exc = assertThrows(RequestBodyValidationException.class,
        () -> delegate.deleteAuthoritySourceFileById(id));

    assertThat(exc.getMessage()).isEqualTo("DELETE is not applicable to consortium shadow copy");
    assertThat(exc.getInvalidParameters()).hasSize(1);
    assertThat(exc.getInvalidParameters().get(0))
        .matches(param -> param.getKey().equals("id") && param.getValue().equals(String.valueOf(id)));
    verifyNoMoreInteractions(service);
    verifyNoInteractions(propagationService);
  }
}
