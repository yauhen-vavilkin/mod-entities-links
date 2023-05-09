package org.folio.entlinks.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SourceReaderTest {

  @Test
  void name() {
    Integer chunkSize = 10000;
    Integer totalRecords = 38456;

    assertEquals(4, totalRecords / chunkSize);
  }
}
