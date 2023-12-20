package org.folio.entlinks.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthoritySourceFileRepository extends JpaCqlRepository<AuthoritySourceFile, UUID> {

  Optional<AuthoritySourceFile> findByName(String name);

  @Query(value = "SELECT nextval(:sequenceName)", nativeQuery = true)
  long getNextSequenceNumber(String sequenceName);
}
