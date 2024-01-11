package org.folio.entlinks.domain.repository;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.entity.projection.AuthorityId;
import org.folio.spring.cql.JpaCqlRepository;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityArchiveRepository extends JpaCqlRepository<AuthorityArchive, UUID>,
    AuthorityArchiveCqlRepository {

  @Query("select aa from AuthorityArchive aa where aa.updatedDate <= :tillDate")
  @QueryHints(@QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "25"))
  Stream<AuthorityArchive> streamByUpdatedTillDate(@Param("tillDate") LocalDateTime tillDate);

  @Query("select a.id as id from AuthorityArchive a")
  Page<AuthorityId> findAllIds(Pageable pageable);
}
