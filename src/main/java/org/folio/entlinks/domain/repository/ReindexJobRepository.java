package org.folio.entlinks.domain.repository;

import java.util.UUID;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReindexJobRepository extends JpaCqlRepository<ReindexJob, UUID> {
}
