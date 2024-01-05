package org.folio.entlinks.controller;

import static org.springframework.http.ResponseEntity.noContent;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.AuthoritySourceFileServiceDelegate;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDto;
import org.folio.entlinks.rest.resource.AuthoritySourceFileApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class AuthoritySourceFilesController implements AuthoritySourceFileApi {

  private final AuthoritySourceFileServiceDelegate delegate;

  @Override
  public ResponseEntity<AuthoritySourceFileDto> createAuthoritySourceFile(AuthoritySourceFilePostDto postDto) {
    var created = delegate.createAuthoritySourceFile(postDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<Void> deleteAuthoritySourceFile(UUID id) {
    delegate.deleteAuthoritySourceFileById(id);
    return noContent().build();
  }

  @Override
  public ResponseEntity<AuthoritySourceFileDto> getAuthoritySourceFile(UUID id) {
    var authoritySourceFile = delegate.getAuthoritySourceFileById(id);
    return ResponseEntity.ok(authoritySourceFile);
  }

  @Override
  public ResponseEntity<Void> patchAuthoritySourceFile(UUID id, AuthoritySourceFilePatchDto authoritySourceFile) {
    delegate.patchAuthoritySourceFile(id, authoritySourceFile);
    return noContent().build();
  }

  @Override
  public ResponseEntity<AuthoritySourceFileDtoCollection> retrieveAuthoritySourceFiles(Integer offset, Integer limit,
                                                                                       String query) {
    var authoritySourceFiles = delegate.getAuthoritySourceFiles(offset, limit, query);
    return ResponseEntity.ok(authoritySourceFiles);
  }
}
