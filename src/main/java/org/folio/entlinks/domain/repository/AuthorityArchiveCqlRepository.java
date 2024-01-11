package org.folio.entlinks.domain.repository;

import org.folio.entlinks.domain.entity.projection.AuthorityIdDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthorityArchiveCqlRepository {

  Page<AuthorityIdDto> findIdsByCql(String cql, Pageable pageable);
}
