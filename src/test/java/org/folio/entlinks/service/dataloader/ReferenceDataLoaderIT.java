package org.folio.entlinks.service.dataloader;

import static org.folio.support.base.TestConstants.authorityNoteTypesEndpoint;
import static org.folio.support.base.TestConstants.authoritySourceFilesEndpoint;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import lombok.SneakyThrows;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@IntegrationTest
@DatabaseCleanup(tables = {DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE,
                           DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE,
                           DatabaseHelper.AUTHORITY_NOTE_TYPE_TABLE})
class ReferenceDataLoaderIT extends IntegrationTestBase {

  @BeforeAll
  static void prepare() {
    setUpTenant(true);
  }

  @Test
  @SneakyThrows
  void testReferenceDataLoaded() {
    // authority note types
    doGet(authorityNoteTypesEndpoint())
      .andExpect(jsonPath("totalRecords", is(1)));

    // authority source files
    doGet(authoritySourceFilesEndpoint())
      .andExpect(jsonPath("totalRecords", is(1)));
  }
}
