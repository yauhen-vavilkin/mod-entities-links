package org.folio.entlinks.repository;

import java.util.List;
import java.util.UUID;
import org.folio.entlinks.model.entity.InstanceLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceLinkRepository extends JpaRepository<InstanceLink, Long> {

  List<InstanceLink> findByInstanceId(UUID instanceId);

}
