package org.folio.entlinks.service.authority;

import static org.mockito.Mockito.verify;

import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.repository.AuthorityArchiveRepository;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityArchiveServiceTest {

  @Mock
  private AuthorityArchiveRepository repository;

  @InjectMocks
  private AuthorityArchiveService service;

  @Test
  void shouldDeleteAuthorityArchive() {
    var archive = new AuthorityArchive();

    service.delete(archive);

    verify(repository).delete(archive);
  }
}
