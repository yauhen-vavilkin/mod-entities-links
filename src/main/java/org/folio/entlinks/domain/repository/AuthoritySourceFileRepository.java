package org.folio.entlinks.domain.repository;

import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthoritySourceFileRepository extends JpaCqlRepository<AuthoritySourceFile, UUID> {
}
