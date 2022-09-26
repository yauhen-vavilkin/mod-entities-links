package org.folio.entlinks.model.projection;

import java.util.UUID;

public interface LinkCountView {

  UUID getId();

  Long getTotalLinks();
}
