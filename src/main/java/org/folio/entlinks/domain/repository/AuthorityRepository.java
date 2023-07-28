package org.folio.entlinks.domain.repository;

import java.util.UUID;
import java.util.stream.Stream;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityRepository extends JpaCqlRepository<Authority, UUID> {

  @Query("select a from Authority a")
  Stream<Authority> streamAll();
}
