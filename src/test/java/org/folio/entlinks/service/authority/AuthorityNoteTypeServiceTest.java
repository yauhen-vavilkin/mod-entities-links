package org.folio.entlinks.service.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityNoteType;
import org.folio.entlinks.domain.repository.AuthorityNoteTypeRepository;
import org.folio.entlinks.exception.AuthorityNoteTypeNotFoundException;
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
class AuthorityNoteTypeServiceTest {

  @Mock
  private AuthorityNoteTypeRepository repository;

  @InjectMocks
  private AuthorityNoteTypeService service;

  @Test
  void shouldGetAllAuthorityNoteTypesByOffsetAndLimit() {
    var expected = new PageImpl<>(List.of(new AuthorityNoteType()));
    when(repository.findAll(any(Pageable.class))).thenReturn(expected);

    var result = service.getAll(0, 10, null);

    assertThat(result).isEqualTo(expected);
    verify(repository).findAll(any(Pageable.class));
  }

  @Test
  void shouldGetAllAuthorityNoteTypesByCqlQuery() {
    var expected = new PageImpl<>(List.of(new AuthorityNoteType()));
    when(repository.findByCql(any(String.class), any(Pageable.class))).thenReturn(expected);

    var result = service.getAll(0, 10, "some_query_string");

    assertThat(result).isEqualTo(expected);
    verify(repository).findByCql(any(String.class), any(Pageable.class));
  }

  @Test
  void shouldGetAuthorityNoteTypeById() {
    var expected = new AuthorityNoteType();
    when(repository.findById(any(UUID.class))).thenReturn(Optional.of(expected));

    var result = service.getById(UUID.randomUUID());

    assertThat(result).isEqualTo(expected);
    verify(repository).findById(any(UUID.class));
  }

  @Test
  void shouldThrowExceptionWhenNoAuthorityNoteTypeExistById() {
    when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());
    var id = UUID.randomUUID();

    assertThrows(AuthorityNoteTypeNotFoundException.class, () -> service.getById(id));
    verify(repository).findById(any(UUID.class));
  }

  @Test
  void shouldCreateAuthorityNoteType() {
    var expected = new AuthorityNoteType();
    when(repository.save(any(AuthorityNoteType.class))).thenReturn(expected);

    var argumentCaptor = ArgumentCaptor.forClass(AuthorityNoteType.class);
    var created = service.create(new AuthorityNoteType());

    assertThat(created).isEqualTo(expected);
    verify(repository).save(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getId()).isNotNull();
  }

  @Test
  void shouldUpdateAuthorityNoteType() {
    var entity = new AuthorityNoteType();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    entity.setName("updated name");
    entity.setSource("updated source");
    var expected = new AuthorityNoteType();
    expected.setId(id);
    when(repository.findById(id)).thenReturn(Optional.of(expected));
    when(repository.save(expected)).thenReturn(expected);

    var updated = service.update(id, entity);

    assertThat(updated).isEqualTo(expected);
    verify(repository).findById(id);
    verify(repository).save(expected);
  }

  @Test
  void shouldThrowExceptionEntityIdDiffersFromProvidedId() {
    var entity = new AuthorityNoteType();
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
  void shouldDeleteAuthorityNoteType() {
    when(repository.existsById(any(UUID.class))).thenReturn(true);
    doNothing().when(repository).deleteById(any(UUID.class));

    service.deleteById(UUID.randomUUID());

    verify(repository).existsById(any(UUID.class));
    verify(repository).deleteById(any(UUID.class));
  }

  @Test
  void shouldThrowExceptionWhenNoEntityExistsToDelete() {
    var id = UUID.randomUUID();
    when(repository.existsById(any(UUID.class))).thenReturn(false);

    var thrown = assertThrows(AuthorityNoteTypeNotFoundException.class, () -> service.deleteById(id));
    assertThat(thrown.getMessage()).containsOnlyOnce(id.toString());
  }
}
