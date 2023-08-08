package org.folio.entlinks.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityRepository extends JpaCqlRepository<Authority, UUID> {

  @Query("select a from Authority a where a.naturalId in :naturalIds")
  List<Authority> findByNaturalIds(@Param("naturalIds") Collection<String> naturalIds);
}
