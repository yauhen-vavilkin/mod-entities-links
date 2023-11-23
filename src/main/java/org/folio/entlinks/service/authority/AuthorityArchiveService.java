package org.folio.entlinks.service.authority;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.repository.AuthorityArchiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Log4j2
public class AuthorityArchiveService {

  private final AuthorityArchiveRepository repository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void delete(AuthorityArchive authorityArchive) {
    log.debug("Deleting authority archive: id = {}", authorityArchive.getId());
    repository.delete(authorityArchive);
  }
}
