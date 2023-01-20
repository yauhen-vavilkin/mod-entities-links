package org.folio.entlinks.domain.entity.projection;

import java.util.UUID;

public interface LinkCountView {

  UUID getId();

  Integer getTotalLinks();
}
