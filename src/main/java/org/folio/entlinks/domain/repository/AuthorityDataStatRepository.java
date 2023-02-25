package org.folio.entlinks.domain.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorityDataStatRepository extends JpaRepository<AuthorityDataStat, UUID> {

  @Query("""
    select a from AuthorityDataStat a
    where a.action = :action
          and a.startedAt >= :startedAtStart
          and a.startedAt <= :startedAtEnd
          and a.authorityData.deleted = false""")
  List<AuthorityDataStat> findActualByActionAndDate(@Param("action") AuthorityDataStatAction action,
                                                    @Param("startedAtStart") Timestamp startedAtStart,
                                                    @Param("startedAtEnd") Timestamp startedAtEnd, Pageable pageable);

}
