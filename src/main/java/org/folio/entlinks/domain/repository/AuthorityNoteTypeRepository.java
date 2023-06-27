package org.folio.entlinks.domain.repository;

import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityNoteType;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityNoteTypeRepository extends JpaCqlRepository<AuthorityNoteType, UUID> {
}
