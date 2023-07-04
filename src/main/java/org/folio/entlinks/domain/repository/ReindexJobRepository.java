package org.folio.entlinks.domain.repository;

import java.util.UUID;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReindexJobRepository extends JpaRepository<ReindexJob, UUID> {
}
