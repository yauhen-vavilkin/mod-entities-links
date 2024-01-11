package org.folio.entlinks.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.projection.AuthorityId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority, UUID>, AuthorityCqlRepository {

  List<Authority> findByNaturalIdInAndDeletedFalse(Collection<String> naturalIds);

  Page<Authority> findAllByDeletedFalse(Pageable pageable);

  @Query("select a.id as id from Authority a where a.deleted = false")
  Page<AuthorityId> findAllIdsByDeletedFalse(Pageable pageable);

  Optional<Authority> findByIdAndDeletedFalse(UUID id);

  List<Authority> findAllByIdInAndDeletedFalse(Collection<UUID> ids);
}
