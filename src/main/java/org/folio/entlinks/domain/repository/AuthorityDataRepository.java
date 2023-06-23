package org.folio.entlinks.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorityDataRepository extends JpaRepository<AuthorityData, UUID> {

  @Modifying
  @Query("update AuthorityData a set a.naturalId = :naturalId where a.id = :id")
  void updateNaturalIdById(@Param("naturalId") String naturalId, @Param("id") UUID id);

  @Modifying
  @Query("update AuthorityData a set a.deleted = true where a.id in :ids")
  void updateDeletedByIdIn(@Param("ids") Collection<UUID> ids);

  @Query("select a from AuthorityData a where a.naturalId in :naturalIds")
  List<AuthorityData> findByNaturalIds(@Param("naturalIds") Collection<String> naturalIds);

  List<AuthorityData> findByIdInAndDeleted(@Param("ids") Collection<UUID> ids, Boolean deleted);
}
