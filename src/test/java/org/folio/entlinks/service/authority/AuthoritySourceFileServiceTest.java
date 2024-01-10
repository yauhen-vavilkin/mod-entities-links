package org.folio.entlinks.service.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.entity.AuthoritySourceFileSource.LOCAL;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.entity.AuthoritySourceFileSource;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthoritySourceFileHridException;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.BadSqlGrammarException;

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

  @ParameterizedTest
  @ValueSource(strings = {"LOCAL", "CONSORTIUM"})
  void shouldCreateAuthoritySourceFile(String source) {
    var code = new AuthoritySourceFileCode();
    code.setCode("code");
    var entity = new AuthoritySourceFile();
    entity.setAuthoritySourceFileCodes(Set.of(code));
    entity.setSource(AuthoritySourceFileSource.valueOf(source));
    var expected = new AuthoritySourceFile(entity);
    when(repository.save(any(AuthoritySourceFile.class))).thenAnswer(i -> i.getArguments()[0]);

    var created = service.create(entity);

    expected.setId(created.getId());
    assertThat(created).isEqualTo(expected);
  }

  @Test
  void shouldNotBePossibleToCreateAuthoritySourceFileOfSourceFolio() {
    var code = new AuthoritySourceFileCode();
    var entity = new AuthoritySourceFile();
    entity.setAuthoritySourceFileCodes(Set.of(code));
    entity.setSource(AuthoritySourceFileSource.FOLIO);

    var thrown = assertThrows(RequestBodyValidationException.class, () -> service.create(entity));

    verifyNoInteractions(repository);
    assertThat(thrown.getInvalidParameters()).hasSize(1);
    assertThat(thrown.getInvalidParameters().get(0).getKey()).isEqualTo("source");
    assertThat(thrown.getInvalidParameters().get(0).getValue()).isEqualTo(entity.getSource().name());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "123", "#", "abc123", "abc def"})
  void shouldNotBePossibleToCreateAuthoritySourceFileWithInvalidCode(String code) {
    var sourceFileCode = new AuthoritySourceFileCode();
    sourceFileCode.setCode(code);
    var entity = new AuthoritySourceFile();
    entity.setAuthoritySourceFileCodes(Set.of(sourceFileCode));
    entity.setSource(LOCAL);

    var thrown = assertThrows(RequestBodyValidationException.class, () -> service.create(entity));

    verifyNoInteractions(repository);
    assertThat(thrown.getInvalidParameters()).hasSize(1);
    assertThat(thrown.getInvalidParameters().get(0).getKey()).isEqualTo("code");
    assertThat(thrown.getInvalidParameters().get(0).getValue()).isEqualTo(code);
  }

  @Test
  void shouldNotBePossibleToCreateLocalAuthoritySourceWithMoreThanOneCode() {
    var code1 = new AuthoritySourceFileCode();
    code1.setCode("code1");
    var code2 = new AuthoritySourceFileCode();
    code2.setCode("code2");
    var entity = new AuthoritySourceFile();
    entity.setAuthoritySourceFileCodes(Set.of(code1, code2));
    entity.setSource(LOCAL);

    var thrown = assertThrows(RequestBodyValidationException.class, () -> service.create(entity));

    verifyNoInteractions(repository);
    assertThat(thrown.getInvalidParameters()).hasSize(1);
    assertThat(thrown.getInvalidParameters().get(0).getKey()).isEqualTo("code");
    assertThat(thrown.getInvalidParameters().get(0).getValue()).contains(code1.getCode()).contains(code2.getCode());
  }

  @Test
  void shouldUpdateAuthoritySourceFile() {
    var entity = new AuthoritySourceFile();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    entity.setName("updated name");
    entity.setSource(LOCAL);
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
    authoritySourceFile.setSource(AuthoritySourceFileSource.FOLIO);

    when(repository.findById(id)).thenReturn(Optional.of(authoritySourceFile));

    var thrown = assertThrows(RequestBodyValidationException.class, () -> service.deleteById(id));

    assertThat(thrown.getMessage()).isEqualTo("Cannot delete Authority source file with source 'folio'");
    verify(repository).findById(id);
    verify(repository, never()).deleteById(any(UUID.class));
  }

  @Test
  void nextHrid_SuccessfulExecution_ReturnsNextHrid() {
    // Arrange
    final var id = UUID.randomUUID();
    var code = new AuthoritySourceFileCode();
    code.setCode("CODE");
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setSequenceName("sequenceName");
    sourceFile.setAuthoritySourceFileCodes(Collections.singleton(code));

    when(repository.getNextSequenceNumber("sequenceName")).thenReturn(10L);
    when(repository.findById(id)).thenReturn(Optional.of(sourceFile));

    // Act
    String nextHrid = service.nextHrid(id);

    // Assert
    assertEquals("CODE10", nextHrid);
  }

  @Test
  void nextHrid_BlankSequenceName_ThrowsException() {
    // Arrange
    final var id = UUID.randomUUID();
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setAuthoritySourceFileCodes(Collections.singleton(new AuthoritySourceFileCode()));

    when(repository.findById(id)).thenReturn(Optional.of(sourceFile));

    // Act & Assert
    assertThrows(AuthoritySourceFileHridException.class, () -> service.nextHrid(id));
  }

  @Test
  void nextHrid_MultipleCodes_ThrowsException() {
    // Arrange
    final var id = UUID.randomUUID();
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setSequenceName("sequenceName");
    sourceFile.setAuthoritySourceFileCodes(Set.of(new AuthoritySourceFileCode(), new AuthoritySourceFileCode()));

    when(repository.findById(id)).thenReturn(Optional.of(sourceFile));

    // Act & Assert
    assertThrows(AuthoritySourceFileHridException.class, () -> service.nextHrid(id));
  }

  @Test
  void nextHrid_DataAccessException_ThrowsException() {
    // Arrange
    final var id = UUID.randomUUID();
    var code = new AuthoritySourceFileCode();
    code.setCode("CODE");
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setSequenceName("sequenceName");
    sourceFile.setAuthoritySourceFileCodes(Collections.singleton(code));

    when(repository.findById(id)).thenReturn(Optional.of(sourceFile));
    when(repository.getNextSequenceNumber("sequenceName")).thenThrow(BadSqlGrammarException.class);

    // Act & Assert
    assertThrows(AuthoritySourceFileHridException.class, () -> service.nextHrid(id));
  }
}
