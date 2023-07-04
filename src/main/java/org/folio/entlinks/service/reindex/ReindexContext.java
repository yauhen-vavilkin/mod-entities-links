package org.folio.entlinks.service.reindex;

import java.util.UUID;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.spring.FolioExecutionContext;

public record ReindexContext(ReindexJob reindexJob, FolioExecutionContext context) {

  public UUID getJobId() {
    return reindexJob.getId();
  }

  public String getTenantId() {
    return context.getTenantId();
  }
}
