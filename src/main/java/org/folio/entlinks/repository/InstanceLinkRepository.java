package org.folio.entlinks.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.projection.LinkCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstanceLinkRepository extends JpaRepository<InstanceAuthorityLink, Long> {

  List<InstanceAuthorityLink> findByInstanceId(UUID instanceId);

  Page<InstanceAuthorityLink> findByAuthorityId(UUID instanceId, Pageable pageable);

  @Modifying
  @Query("DELETE FROM InstanceAuthorityLink il WHERE il.authorityId IN :authorityIds")
  void deleteByAuthorityIdIn(@Param("authorityIds") Set<UUID> authorityIds);

  @Query("SELECT il.authorityId AS id,"
    + " COUNT(DISTINCT il.instanceId) AS totalLinks"
    + " FROM InstanceAuthorityLink il WHERE il.authorityId IN :authorityIds"
    + " GROUP BY id")
  List<LinkCountView> countLinksByAuthorityIds(@Param("authorityIds") Set<UUID> authorityIds);

  @Query("SELECT il.authorityId FROM InstanceAuthorityLink il WHERE il.authorityId IN :authorityIds")
  Set<UUID> findAuthorityIdsWithLinks(@Param("authorityIds") Set<UUID> authorityIds);

  @Modifying
  @Query("UPDATE InstanceAuthorityLink il SET il.authorityNaturalId = :naturalId, il.bibRecordSubfields = :subfields "
    + "WHERE il.authorityId = :authorityId AND il.bibRecordTag = :tag")
  void updateSubfieldsAndNaturalId(@Param("subfields") char[] subfields, @Param("naturalId") String naturalId,
                                   @Param("authorityId") UUID authorityId, @Param("tag") String tag);

  @Modifying
  @Query("UPDATE InstanceAuthorityLink il SET il.authorityNaturalId = :naturalId WHERE il.authorityId = :authorityId")
  void updateNaturalId(@Param("naturalId") String naturalId, @Param("authorityId") UUID authorityId);
}
