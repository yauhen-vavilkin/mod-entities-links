package org.folio.entlinks.controller;

import java.util.UUID;
import org.folio.entlinks.domain.dto.Authority;
import org.folio.entlinks.domain.dto.AuthorityCollection;
import org.folio.entlinks.rest.resource.AuthorityStorageApi;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class AuthorityStorageController implements AuthorityStorageApi {

  @Override
  public ResponseEntity<Authority> createAuthority(Authority authority) {
    return AuthorityStorageApi.super.createAuthority(authority);
  }

  @Override
  public ResponseEntity<Void> deleteAuthority(UUID id) {
    return AuthorityStorageApi.super.deleteAuthority(id);
  }

  @Override
  public ResponseEntity<Authority> getAuthority(UUID id) {
    return AuthorityStorageApi.super.getAuthority(id);
  }

  @Override
  public ResponseEntity<AuthorityCollection> retrieveAuthorities(Integer offset, Integer limit, String query) {
    return AuthorityStorageApi.super.retrieveAuthorities(offset, limit, query);
  }

  @Override
  public ResponseEntity<Void> updateAuthority(UUID id, Authority authority) {
    return AuthorityStorageApi.super.updateAuthority(id, authority);
  }
}
