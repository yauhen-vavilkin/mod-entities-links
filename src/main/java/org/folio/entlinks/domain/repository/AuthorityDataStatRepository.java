package org.folio.entlinks.domain.repository;

import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityDataStatRepository extends JpaRepository<AuthorityDataStat, UUID> {
}
