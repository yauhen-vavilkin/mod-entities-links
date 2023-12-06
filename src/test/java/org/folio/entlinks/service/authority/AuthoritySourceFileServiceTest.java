package org.folio.entlinks.service.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthoritySourceFileServiceTest {

  @Mock
  private AuthoritySourceFileRepository repository;

  @Mock
  private AuthoritySourceFileMapper mapper;

  @InjectMocks
  private AuthoritySourceFileService service;

  @Test
  void shouldGetAllAuthoritySourceFilesByOffsetAndLimit() {
    var expected = new PageImpl<>(List.of(new AuthoritySourceFile()));
    when(repository.findAll(any(Pageable.class))).thenReturn(expected);

    var result = service.getAll(0, 10, null);

    assertThat(result).isEqualTo(expected);
    verify(repository).findAll(any(Pageable.class));
  }

  @Test
  void shouldGetAllAuthoritySourceFilesByCqlQuery() {
    var expected = new PageImpl<>(List.of(new AuthoritySourceFile()));
    when(repository.findByCql(any(String.class), any(Pageable.class))).thenReturn(expected);

    var result = service.getAll(0, 10, "some_query_string");

    assertThat(result).isEqualTo(expected);
    verify(repository).findByCql(any(String.class), any(Pageable.class));
  }

  @Test
  void shouldGetAuthoritySourceFileById() {
    var expected = new AuthoritySourceFile();
    when(repository.findById(any(UUID.class))).thenReturn(Optional.of(expected));

    var result = service.getById(UUID.randomUUID());

    assertThat(result).isEqualTo(expected);
    verify(repository).findById(any(UUID.class));
  }

  @Test
  void shouldThrowExceptionWhenNoAuthoritySourceFileExistById() {
    when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());
    var id = UUID.randomUUID();

    assertThrows(AuthoritySourceFileNotFoundException.class, () -> service.getById(id));
    verify(repository).findById(any(UUID.class));
  }

  @Test
  void shouldGetAuthoritySourceFileByName() {
    var entity = new AuthoritySourceFile();
    var typeName = "type_name";
    when(repository.findByName(typeName)).thenReturn(Optional.of(entity));

    var found = service.getByName(typeName);

    assertEquals(entity, found);
    verify(repository).findByName(typeName);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotGetAuthoritySourceFileForNullName() {
    var notFound = service.getByName(null);

    assertNull(notFound);
    verifyNoInteractions(repository);
  }

  @Test
  void shouldCreateAuthoritySourceFile() {
    var code = new AuthoritySourceFileCode();
    var entity = new AuthoritySourceFile();
    entity.setAuthoritySourceFileCodes(Set.of(code));
    var expected = new AuthoritySourceFile();
    when(repository.save(any(AuthoritySourceFile.class))).thenReturn(expected);
    var argumentCaptor = ArgumentCaptor.forClass(AuthoritySourceFile.class);

    var created = service.create(entity);

    assertThat(created).isEqualTo(expected);
    verify(repository).save(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getId()).isNotNull();
  }

  @Test
  void shouldUpdateAuthoritySourceFile() {
    var entity = new AuthoritySourceFile();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    entity.setName("updated name");
    entity.setSource("updated source");
    var codeNew = new AuthoritySourceFileCode();
    codeNew.setCode("codeNew");
    entity.addCode(codeNew);
    var codeExisting = new AuthoritySourceFileCode();
    codeExisting.setCode("codeExisting");
    var expected = new AuthoritySourceFile();
    expected.setId(id);
    expected.addCode(codeExisting);

    when(repository.findById(id)).thenReturn(Optional.of(expected));
    when(repository.save(expected)).thenReturn(expected);
    when(mapper.toDtoCodes(entity.getAuthoritySourceFileCodes())).thenReturn(List.of(codeNew.getCode()));
    when(mapper.toDtoCodes(expected.getAuthoritySourceFileCodes())).thenReturn(List.of(codeExisting.getCode()));

    var updated = service.update(id, entity);

    assertThat(updated).isEqualTo(expected);
    assertThat(updated.getAuthoritySourceFileCodes()).isEqualTo(Set.of(codeNew));
    verify(repository).findById(id);
    verify(repository).save(expected);
  }

  @Test
  void shouldThrowExceptionEntityIdDiffersFromProvidedId() {
    var entity = new AuthoritySourceFile();
    UUID id = UUID.randomUUID();
    UUID differentId = UUID.randomUUID();
    entity.setId(id);

    var thrown = assertThrows(RequestBodyValidationException.class, () -> service.update(differentId, entity));

    assertThat(thrown.getInvalidParameters()).hasSize(1);
    assertThat(thrown.getInvalidParameters().get(0).getKey()).isEqualTo("id");
    assertThat(thrown.getInvalidParameters().get(0).getValue()).isEqualTo(id.toString());
    verifyNoInteractions(repository);
  }

  @Test
  void shouldDeleteAuthoritySourceFile() {
    var id = UUID.randomUUID();
    var authoritySourceFile = new AuthoritySourceFile();
    authoritySourceFile.setId(id);
    authoritySourceFile.setType("non-folio");

    when(repository.findById(any(UUID.class))).thenReturn(Optional.of(authoritySourceFile));
    doNothing().when(repository).deleteById(any(UUID.class));

    service.deleteById(UUID.randomUUID());

    verify(repository).findById(any(UUID.class));
    verify(repository).deleteById(any(UUID.class));
  }

  @Test
  void shouldThrowExceptionWhenNoEntityExistsToDelete() {
    var id = UUID.randomUUID();

    when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());

    var thrown = assertThrows(AuthoritySourceFileNotFoundException.class, () -> service.deleteById(id));

    assertThat(thrown.getMessage()).containsOnlyOnce(id.toString());
    verify(repository, never()).deleteById(any(UUID.class));
  }

  @Test
  void shouldNotDeleteWhenFolioType() {
    UUID id = UUID.randomUUID();
    var authoritySourceFile = new AuthoritySourceFile();
    authoritySourceFile.setId(id);
    authoritySourceFile.setSource("folio");

    when(repository.findById(id)).thenReturn(Optional.of(authoritySourceFile));

    var thrown = assertThrows(RequestBodyValidationException.class, () -> service.deleteById(id));

    assertThat(thrown.getMessage()).isEqualTo("Cannot delete Authority source file with source 'folio'");
    verify(repository).findById(id);
    verify(repository, never()).deleteById(any(UUID.class));
  }
}
