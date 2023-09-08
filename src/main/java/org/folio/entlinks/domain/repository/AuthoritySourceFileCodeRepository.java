package org.folio.entlinks.domain.repository;

import java.util.Optional;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthoritySourceFileCodeRepository extends JpaRepository<AuthoritySourceFileCode, Integer> {

  Optional<AuthoritySourceFileCode> findFirstByCodeStartsWith(String code);
}
