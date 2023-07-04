package org.folio.entlinks.service.reindex;

import lombok.Getter;

public class ReindexJobProgressTracker {
  private final @Getter int totalRecords;
  private @Getter int processedCount;

  public ReindexJobProgressTracker(int totalRecords) {
    this.totalRecords = totalRecords;
    this.processedCount = 0;
  }

  public synchronized void incrementProcessedCount() {
    processedCount++;
    System.out.println("Progress: " + processedCount + " / " + totalRecords + " records processed");
  }

}
