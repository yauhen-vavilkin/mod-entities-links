package org.folio.entlinks.controller;

import static org.springframework.http.ResponseEntity.noContent;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.AuthorityNoteTypeServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityNoteTypeDto;
import org.folio.entlinks.domain.dto.AuthorityNoteTypeDtoCollection;
import org.folio.entlinks.rest.resource.AuthorityNoteTypeApi;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class AuthorityNoteTypesController implements AuthorityNoteTypeApi {

  private final AuthorityNoteTypeServiceDelegate delegate;

  @Override
  public ResponseEntity<AuthorityNoteTypeDto> createAuthorityNoteType(AuthorityNoteTypeDto authorityNoteType) {
    var created = delegate.createAuthorityNoteType(authorityNoteType);
    return ResponseEntity.ok(created);
  }

  @Override
  public ResponseEntity<Void> deleteAuthorityNoteType(UUID id) {
    delegate.deleteAuthorityNoteTypeById(id);
    return noContent().build();
  }

  @Override
  public ResponseEntity<AuthorityNoteTypeDto> getAuthorityNoteType(UUID id) {
    var authorityNoteType = delegate.getAuthorityNoteTypeById(id);
    return ResponseEntity.ok(authorityNoteType);
  }

  @Override
  public ResponseEntity<AuthorityNoteTypeDtoCollection> retrieveAuthorityNoteTypes(Integer offset, Integer limit,
                                                                                   String query) {
    var authorityNoteTypes = delegate.getAuthorityNoteTypes(offset, limit, query);
    return ResponseEntity.ok(authorityNoteTypes);
  }

  @Override
  public ResponseEntity<Void> updateAuthorityNoteType(UUID id, AuthorityNoteTypeDto authorityNoteType) {
    delegate.updateAuthorityNoteType(id, authorityNoteType);
    return ResponseEntity.accepted().build();
  }
}
