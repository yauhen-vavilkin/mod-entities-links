package org.folio.entlinks.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChunkPreparationTest {

  @Test
  void name() {
    Integer chunkSize = 10000;
    Integer totalRecords = 38456;
    var chunkAmount = ChunkPreparation.getChunkAmount(chunkSize, totalRecords);
    assertEquals(4, chunkAmount);
  }
}
