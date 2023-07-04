package org.folio.entlinks.controller;

import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.ReindexServiceDelegate;
import org.folio.entlinks.domain.dto.ReindexJobDto;
import org.folio.entlinks.domain.dto.ReindexJobDtoCollection;
import org.folio.entlinks.rest.resource.AuthorityStorageReindexApi;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class AuthorityStorageReindexController implements AuthorityStorageReindexApi {

  private final ReindexServiceDelegate reindexServiceDelegate;

  @Override
  public ResponseEntity<ReindexJobDtoCollection> getReindexJobs(String query, Integer offset, Integer limit) {
    return AuthorityStorageReindexApi.super.getReindexJobs(query, offset, limit);
  }

  @Override
  public ResponseEntity<ReindexJobDto> submitReindexJob() {
    return ResponseEntity.ok(reindexServiceDelegate.startAuthoritiesReindex());
  }
}
