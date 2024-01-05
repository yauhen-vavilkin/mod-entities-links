package org.folio.entlinks.service.consortium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.service.consortium.ConsortiumAuthorityPropagationServiceIT.COLLEGE_TENANT_ID;
import static org.folio.entlinks.service.consortium.ConsortiumAuthorityPropagationServiceIT.UNIVERSITY_TENANT_ID;
import static org.folio.support.DatabaseHelper.AUTHORITY_ARCHIVE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_DATA_STAT_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_NOTE_TYPE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_TABLE;
import static org.folio.support.base.TestConstants.CENTRAL_TENANT_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

@IntegrationTest
@DatabaseCleanup(tables = {AUTHORITY_DATA_STAT_TABLE, AUTHORITY_ARCHIVE_TABLE, AUTHORITY_TABLE,
  AUTHORITY_SOURCE_FILE_CODE_TABLE, AUTHORITY_SOURCE_FILE_TABLE, AUTHORITY_NOTE_TYPE_TABLE},
                 tenants = {CENTRAL_TENANT_ID, COLLEGE_TENANT_ID, UNIVERSITY_TENANT_ID})
class ConsortiumAuthorityPropagationServiceIT extends IntegrationTestBase {

  public static final String COLLEGE_TENANT_ID = "college";
  public static final String UNIVERSITY_TENANT_ID = "university";

  private static final String CONSORTIUM_SOURCE_PREFIX = "CONSORTIUM-";
  private static final UUID AUTHORITY_ID = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f325e");

  @BeforeAll
  static void beforeAll() {
    setUpConsortium(CENTRAL_TENANT_ID, List.of(COLLEGE_TENANT_ID, UNIVERSITY_TENANT_ID), true);
  }

  @Test
  @SneakyThrows
  void testAuthorityCreatePropagation() {
    var dto = new AuthorityDto()
      .id(AUTHORITY_ID)
      .version(0)
      .source("MARC")
      .naturalId("ns12345")
      .personalName("Nikola Tesla");
    doPost(authorityEndpoint(), dto, tenantHeaders(CENTRAL_TENANT_ID));
    var centralAuthority = requestAuthority(CENTRAL_TENANT_ID);
    assertThat(centralAuthority)
      .extracting(AuthorityDto::getId, AuthorityDto::getSource, AuthorityDto::getNaturalId,
        AuthorityDto::getPersonalName)
      .containsExactly(dto.getId(), dto.getSource(), dto.getNaturalId(), dto.getPersonalName());

    awaitUntilAsserted(() ->
      assertEquals(1, databaseHelper.countRows(AUTHORITY_TABLE, COLLEGE_TENANT_ID)));
    var collegeAuthority = requestAuthority(COLLEGE_TENANT_ID);
    assertThat(collegeAuthority)
      .extracting(AuthorityDto::getId, AuthorityDto::getSource, AuthorityDto::getNaturalId,
        AuthorityDto::getPersonalName)
      .containsExactly(dto.getId(), CONSORTIUM_SOURCE_PREFIX + dto.getSource(), dto.getNaturalId(),
          dto.getPersonalName());

    awaitUntilAsserted(() ->
      assertEquals(1, databaseHelper.countRows(AUTHORITY_TABLE, UNIVERSITY_TENANT_ID)));
    var universityAuthority = requestAuthority(UNIVERSITY_TENANT_ID);
    assertThat(universityAuthority)
      .extracting(AuthorityDto::getId, AuthorityDto::getSource, AuthorityDto::getNaturalId,
        AuthorityDto::getPersonalName)
      .containsExactly(dto.getId(), CONSORTIUM_SOURCE_PREFIX + dto.getSource(), dto.getNaturalId(),
          dto.getPersonalName());

  }

  @Test
  @SneakyThrows
  void testAuthorityDeletePropagation() {
    var dto = new AuthorityDto()
      .id(AUTHORITY_ID)
      .version(0)
      .source("MARC")
      .naturalId("ns12345")
      .personalName("Nikola Tesla");
    doPost(authorityEndpoint(), dto, tenantHeaders(CENTRAL_TENANT_ID));
    assertThat(requestAuthority(CENTRAL_TENANT_ID)).isNotNull();
    doDelete(authorityEndpoint(AUTHORITY_ID), tenantHeaders(CENTRAL_TENANT_ID));
    tryGet(authorityEndpoint(AUTHORITY_ID), tenantHeaders(CENTRAL_TENANT_ID)).andExpect(status().isNotFound());

    awaitUntilAsserted(() ->
        assertEquals(0, databaseHelper.countRows(AUTHORITY_TABLE, CENTRAL_TENANT_ID)));
    awaitUntilAsserted(() ->
      assertEquals(0, databaseHelper.countRows(AUTHORITY_TABLE, COLLEGE_TENANT_ID)));
    awaitUntilAsserted(() ->
      assertEquals(0, databaseHelper.countRows(AUTHORITY_TABLE, UNIVERSITY_TENANT_ID)));
  }

  @Test
  @SneakyThrows
  void testAuthorityUpdatePropagation() {
    var dto = new AuthorityDto()
      .id(AUTHORITY_ID)
      .version(0)
      .source("MARC")
      .naturalId("ns12345")
      .personalName("Nikola Tesla");
    doPost(authorityEndpoint(), dto, tenantHeaders(CENTRAL_TENANT_ID));
    assertThat(requestAuthority(CENTRAL_TENANT_ID)).isNotNull();
    awaitUntilAsserted(() -> assertNotNull(requestAuthority(COLLEGE_TENANT_ID)));
    awaitUntilAsserted(() -> assertNotNull(requestAuthority(UNIVERSITY_TENANT_ID)));
    doPut(authorityEndpoint(AUTHORITY_ID), dto.personalName("updated"), tenantHeaders(CENTRAL_TENANT_ID));

    var centralAuthority = requestAuthority(CENTRAL_TENANT_ID);
    assertThat(centralAuthority)
      .extracting(AuthorityDto::getId, AuthorityDto::getSource, AuthorityDto::getNaturalId,
        AuthorityDto::getPersonalName)
      .containsExactly(dto.getId(), dto.getSource(), dto.getNaturalId(), dto.getPersonalName());

    awaitUntilAsserted(() ->
      assertEquals(1, databaseHelper.countRowsWhere(AUTHORITY_TABLE, COLLEGE_TENANT_ID, "heading = 'updated'")));
    var collegeAuthority = requestAuthority(COLLEGE_TENANT_ID);
    assertThat(collegeAuthority)
      .extracting(AuthorityDto::getId, AuthorityDto::getSource, AuthorityDto::getNaturalId,
        AuthorityDto::getPersonalName)
      .containsExactly(dto.getId(), CONSORTIUM_SOURCE_PREFIX + dto.getSource(), dto.getNaturalId(),
          dto.getPersonalName());

    awaitUntilAsserted(() ->
      assertEquals(1, databaseHelper.countRowsWhere(AUTHORITY_TABLE, UNIVERSITY_TENANT_ID, "heading = 'updated'")));
    var universityAuthority = requestAuthority(UNIVERSITY_TENANT_ID);
    assertThat(universityAuthority)
      .extracting(AuthorityDto::getId, AuthorityDto::getSource, AuthorityDto::getNaturalId,
        AuthorityDto::getPersonalName)
      .containsExactly(dto.getId(), CONSORTIUM_SOURCE_PREFIX + dto.getSource(), dto.getNaturalId(),
          dto.getPersonalName());

  }

  private AuthorityDto requestAuthority(String tenantId)
    throws UnsupportedEncodingException, JsonProcessingException {
    var response = doGet(authorityEndpoint(AUTHORITY_ID), tenantHeaders(tenantId)).andReturn()
      .getResponse()
      .getContentAsString();
    return objectMapper.readValue(response, AuthorityDto.class);
  }

  private HttpHeaders tenantHeaders(String tenant) {
    var httpHeaders = defaultHeaders();
    httpHeaders.put(XOkapiHeaders.TENANT, Collections.singletonList(tenant));
    return httpHeaders;
  }

}
