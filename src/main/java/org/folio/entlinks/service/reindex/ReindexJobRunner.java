package org.folio.entlinks.service.reindex;

import org.folio.entlinks.domain.entity.ReindexJob;

public interface ReindexJobRunner {

  void startReindex(ReindexJob reindexJob);

}
