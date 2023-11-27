package org.folio.entlinks.service.consortium.propagation.model;

import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;

public record LinksPropagationData(UUID instanceId, List<InstanceAuthorityLink> links) {
}
