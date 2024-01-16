package org.folio.entlinks;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@Log4j2
@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE
})
class EntityLinksApplicationIT extends IntegrationTestBase {

  @Autowired
  private EntityLinksApplication entityLinksApplication;

  @Test
  void contextLoads() {
    assertNotNull(entityLinksApplication);
  }

  @Test
  @SneakyThrows
  void healthEndpointWorks() {
    mockMvc.perform(MockMvcRequestBuilders.get("/admin/health"))
      .andExpect(status().is2xxSuccessful())
      .andExpect(jsonPath("$.status", equalTo("UP")));
  }

  @Test
  @SneakyThrows
  void postTenantEndpoint_enablesModuleForTenant() {
    mockMvc.perform(MockMvcRequestBuilders.post("/_/tenant")
        .headers(defaultHeaders())
        .content("""
          {
              "module_to": "mod-1.0.0",
              "parameters": [
                  {
                      "key": "loadReference",
                      "value": "true"
                  }
              ]
          }
          """))
      .andExpect(status().is2xxSuccessful());
  }

  @Test
  @SneakyThrows
  void postTenantEndpoint_disablesModuleForTenant() {
    log.info("Enable module");
    mockMvc.perform(MockMvcRequestBuilders.post("/_/tenant")
        .headers(defaultHeaders())
        .content("""
          {
              "module_to": "mod-1.0.0",
              "parameters": [
                  {
                      "key": "loadReference",
                      "value": "true"
                  }
              ]
          }
          """))
      .andExpect(status().is2xxSuccessful());

    log.info("Disable module");
    mockMvc.perform(MockMvcRequestBuilders.post("/_/tenant")
        .headers(defaultHeaders())
        .content("""
          {
              "module_from": "mod-1.0.0",
              "purge": "true"
          }
          """))
      .andExpect(status().is2xxSuccessful());
  }
}
