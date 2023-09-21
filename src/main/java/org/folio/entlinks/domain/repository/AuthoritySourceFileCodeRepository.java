package org.folio.entlinks.domain.repository;

import java.util.Optional;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthoritySourceFileCodeRepository extends JpaRepository<AuthoritySourceFileCode, Integer> {

  @Query(value = """
      SELECT * FROM authority_source_file_code sfc
      where :pattern like (sfc.code || '%')
      ORDER BY LENGTH(sfc.code) desc
      LIMIT 1
      """,
      nativeQuery = true)
  Optional<AuthoritySourceFileCode> findByCodeAsPrefixFor(@Param("pattern") String pattern);
}
