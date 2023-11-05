package org.folio.entlinks.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.TestDataUtils.AuthorityTestData.authoritySourceFile;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.UsersClient;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE,
  DatabaseHelper.AUTHORITY_DATA_STAT_TABLE,
  DatabaseHelper.AUTHORITY_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE})
@TestPropertySource(properties = "folio.system-user.enabled=false")
class DisabledSystemUserIT extends IntegrationTestBase {

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @BeforeEach
  void setUp(@Autowired ApplicationContext ctx) {
    assertThatThrownBy(() -> ctx.getBean(AuthnClient.class)).isInstanceOf(NoSuchBeanDefinitionException.class);
    assertThatThrownBy(() -> ctx.getBean(UsersClient.class)).isInstanceOf(NoSuchBeanDefinitionException.class);
    assertThatThrownBy(() -> ctx.getBean(PermissionsClient.class)).isInstanceOf(NoSuchBeanDefinitionException.class);
  }

  @Test
  @DisplayName("Get Collection: find all Authority entities")
  void getCollection_positive_entitiesFound() throws Exception {
    var createdEntities = createAuthorities();

    tryGet(authorityEndpoint())
      .andExpect(status().isOk())
      .andExpect(jsonPath("totalRecords", is(createdEntities.size())))
      .andExpect(jsonPath("authorities[0].metadata", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("authorities[0].metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.updatedByUserId", is(USER_ID)));
  }

  private List<Authority> createAuthorities() {
    createSourceFile();
    var entity1 = createAuthority(0);
    var entity2 = createAuthority(1);
    var entity3 = createAuthority(2);

    return List.of(entity1, entity2, entity3);
  }

  private Authority createAuthority(int authorityNum) {
    var entity = authority(authorityNum, 0);
    databaseHelper.saveAuthority(TENANT_ID, entity);
    return entity;
  }

  private void createSourceFile() {
    var entity = authoritySourceFile(0);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, entity);

    entity.getAuthoritySourceFileCodes().forEach(code ->
        databaseHelper.saveAuthoritySourceFileCode(TENANT_ID, entity.getId(), code));
  }
}
