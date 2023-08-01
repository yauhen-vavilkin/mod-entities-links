package org.folio.entlinks.service.reindex;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ReindexJobProgressTracker {
  private final AtomicInteger totalRecords;
  private final AtomicInteger processedCount;

  public ReindexJobProgressTracker(int totalRecords) {
    this.totalRecords = new AtomicInteger(totalRecords);
    this.processedCount = new AtomicInteger(0);
  }

  public synchronized void incrementProcessedCount() {
    processedCount.incrementAndGet();
    log.debug("Progress: {} / {} records processed", processedCount, totalRecords);
  }

  public int getTotalRecords() {
    return totalRecords.intValue();
  }

  public int getProcessedCount() {
    return processedCount.intValue();
  }
}
