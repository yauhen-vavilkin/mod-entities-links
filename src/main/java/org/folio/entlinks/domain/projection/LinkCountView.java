package org.folio.entlinks.domain.projection;

import java.util.UUID;

public interface LinkCountView {

  UUID getId();

  Long getTotalLinks();
}
