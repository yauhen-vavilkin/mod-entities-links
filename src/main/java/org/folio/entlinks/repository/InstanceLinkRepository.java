package org.folio.entlinks.repository;

import java.util.List;
import java.util.UUID;
import org.folio.entlinks.model.entity.InstanceLink;
import org.folio.entlinks.model.projection.LinkCountView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstanceLinkRepository extends JpaRepository<InstanceLink, Long> {

  List<InstanceLink> findByInstanceId(UUID instanceId);

  @Query("SELECT il.authorityId AS id,"
      + " COUNT(DISTINCT il.instanceId) AS totalLinks"
      + " FROM InstanceLink il WHERE il.authorityId IN :ids"
      + " GROUP BY id ORDER BY totalLinks DESC")
  List<LinkCountView> countLinksByAuthorityIds(@Param("ids") List<UUID> ids);

}
