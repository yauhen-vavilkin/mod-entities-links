package org.folio.entlinks.domain.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;

public interface AuthorityDataStatRepository extends JpaRepository<AuthorityDataStat, UUID> {

  List<AuthorityDataStat> findByActionAndStartedAtGreaterThanEqualAndStartedAtLessThanEqual(
    AuthorityDataStatAction action,
    @Nullable Timestamp startedAtStart,
    @Nullable Timestamp startedAtEnd,
    Pageable pageable);
}
