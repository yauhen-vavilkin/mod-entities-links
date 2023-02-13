package org.folio.entlinks.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.projection.LinkCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstanceLinkRepository extends JpaRepository<InstanceAuthorityLink, Long>,
  JpaSpecificationExecutor<InstanceAuthorityLink> {

  List<InstanceAuthorityLink> findByInstanceId(UUID instanceId);

  @Query("select l from InstanceAuthorityLink l where l.authorityData.id = :id order by l.id")
  Page<InstanceAuthorityLink> findByAuthorityId(@Param("id") UUID id, Pageable pageable);

  @Query("select l.authorityData.id as id, count(distinct l.instanceId) as totalLinks"
    + " from InstanceAuthorityLink l where l.authorityData.id in :authorityIds"
    + " group by id")
  List<LinkCountView> countLinksByAuthorityIds(@Param("authorityIds") Set<UUID> authorityIds);

  @Modifying
  @Query("update InstanceAuthorityLink l SET l.bibRecordSubfields = :subfields "
    + "where l.authorityData.id = :authorityId and l.bibRecordTag = :tag")
  void updateSubfieldsByAuthorityIdAndTag(@Param("subfields") char[] subfields, @Param("authorityId") UUID authorityId,
                                          @Param("tag") String tag);

  @Modifying
  @Query("delete from InstanceAuthorityLink i where i.authorityData.id in :authorityIds")
  void deleteByAuthorityIds(@Param("authorityIds") Collection<UUID> authorityIds);

}
