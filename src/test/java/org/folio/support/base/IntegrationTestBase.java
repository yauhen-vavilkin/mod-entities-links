package org.folio.support.base;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.folio.support.JsonTestUtils.asJson;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.core.ThrowingRunnable;
import org.folio.entlinks.client.ConsortiumTenantsClient;
import org.folio.entlinks.client.UserTenantsClient;
import org.folio.entlinks.service.reindex.event.DomainEvent;
import org.folio.entlinks.service.reindex.event.DomainEventType;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.test.extension.EnableKafka;
import org.folio.spring.test.extension.EnablePostgres;
import org.folio.spring.test.extension.impl.OkapiConfiguration;
import org.folio.spring.test.extension.impl.OkapiExtension;
import org.folio.support.DatabaseHelper;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@EnableKafka
@EnablePostgres
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(IntegrationTestBase.KafkaTemplateTestConfiguration.class)
public class IntegrationTestBase {

  protected static final String DOMAIN_EVENT_HEADER_KEY = "domain-event-type";
  protected static MockMvc mockMvc;
  protected static OkapiConfiguration okapi;
  protected static KafkaTemplate<String, String> kafkaTemplate;
  protected static ObjectMapper objectMapper;
  protected static DatabaseHelper databaseHelper;

  @RegisterExtension
  static OkapiExtension okapiExtension = new OkapiExtension();

  @SneakyThrows
  protected static void setUpTenant() {
    setUpTenant(false);
  }

  @SneakyThrows
  protected static void setUpTenant(boolean loadReference) {
    setUpTenant(TENANT_ID, loadReference);
  }

  @SneakyThrows
  protected static void setUpTenant(String tenantId, boolean loadReference) {
    var httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.add(XOkapiHeaders.TENANT, tenantId);
    httpHeaders.add(XOkapiHeaders.USER_ID, USER_ID);
    httpHeaders.add(XOkapiHeaders.URL, okapi.getOkapiUrl());

    var tenantAttributes = new TenantAttributes().moduleTo("mod-entities-links")
      .addParametersItem(new Parameter("loadReference").value(String.valueOf(loadReference)));
    doPost("/_/tenant", tenantAttributes, httpHeaders);
    mockGet("/user-tenants", "", SC_NOT_FOUND, okapi.wireMockServer());
  }

  @SneakyThrows
  protected static void setUpConsortium(String centralTenantId, List<String> memberTenantIds, boolean loadReference) {
    setUpTenant(centralTenantId, loadReference);
    memberTenantIds.forEach(tenantId -> setUpTenant(tenantId, loadReference));
    var consortiumId = UUID.randomUUID().toString();
    var userTenants = new UserTenantsClient.UserTenants(
      List.of(new UserTenantsClient.UserTenant(centralTenantId, consortiumId)));
    mockGet("/user-tenants", objectMapper.writeValueAsString(userTenants), SC_OK, okapi.wireMockServer());
    var consortiumTenantList = memberTenantIds.stream()
      .map(s -> new ConsortiumTenantsClient.ConsortiumTenant(s, false))
      .collect(Collectors.toList());
    consortiumTenantList.add(new ConsortiumTenantsClient.ConsortiumTenant(centralTenantId, true));
    var consortiumTenants = new ConsortiumTenantsClient.ConsortiumTenants(consortiumTenantList);
    mockGet("/consortia/" + consortiumId + "/tenants", objectMapper.writeValueAsString(consortiumTenants), SC_OK,
      okapi.wireMockServer());
  }

  protected static void mockGet(String url, String body, int status, WireMockServer mockServer) {
    mockServer.stubFor(WireMock.get(urlPathEqualTo(url))
      .willReturn(aResponse().withBody(body)
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(status)));
  }

  protected static HttpHeaders defaultHeaders() {
    var httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.add(XOkapiHeaders.TENANT, TENANT_ID);
    httpHeaders.add(XOkapiHeaders.USER_ID, USER_ID);
    httpHeaders.add(XOkapiHeaders.URL, okapi.getOkapiUrl());

    return httpHeaders;
  }

  //use if params contain special characters that should be encoded
  @SneakyThrows
  protected static ResultActions perform(MockHttpServletRequestBuilder rb) {
    return tryDoHttpMethod(rb, null);
  }

  @SneakyThrows
  protected static ResultActions tryDelete(String uri, Object... args) {
    return tryDelete(uri, defaultHeaders(), args);
  }

  @SneakyThrows
  protected static ResultActions tryDelete(String uri, HttpHeaders headers, Object... args) {
    return tryDoHttpMethod(delete(uri, args), null, headers);
  }

  @SneakyThrows
  protected static ResultActions doDelete(String uri, HttpHeaders headers, Object... args) {
    return tryDelete(uri, headers, args).andExpect(status().is2xxSuccessful());
  }

  @SneakyThrows
  protected static ResultActions doDelete(String uri, Object... args) {
    return doDelete(uri, defaultHeaders(), args);
  }

  @SneakyThrows
  protected static ResultActions tryGet(String uri, Object... args) {
    return tryGet(uri, defaultHeaders(), args);
  }

  @SneakyThrows
  protected static ResultActions tryGet(String uri, HttpHeaders headers, Object... args) {
    return tryDoHttpMethod(get(uri, args), null, headers);
  }


  @SneakyThrows
  protected static ResultActions doGet(String uri, Object... args) {
    return doGet(uri, defaultHeaders(), args);
  }

  @SneakyThrows
  protected static ResultActions doGet(String uri, HttpHeaders headers, Object... args) {
    return tryGet(uri, headers, args).andExpect(status().isOk());
  }

  @SneakyThrows
  protected static ResultActions tryPut(String uri, Object body, HttpHeaders headers, Object... args) {
    return tryDoHttpMethod(put(uri, args), body, headers);
  }

  @SneakyThrows
  protected static ResultActions tryPut(String uri, Object body, Object... args) {
    return tryPut(uri, body, defaultHeaders(), args);
  }

  @SneakyThrows
  protected static ResultActions doPut(String uri, Object body, HttpHeaders headers, Object... args) {
    return tryPut(uri, body, headers, args).andExpect(status().is2xxSuccessful());
  }

  @SneakyThrows
  protected static ResultActions doPut(String uri, Object body, Object... args) {
    return doPut(uri, body, defaultHeaders(), args);
  }

  @SneakyThrows
  protected static ResultActions tryPatch(String uri, Object body, Object... args) {
    return tryDoHttpMethod(patch(uri, args), body);
  }

  @SneakyThrows
  protected static ResultActions doPatch(String uri, Object body, Object... args) {
    return tryPatch(uri, body, args).andExpect(status().is2xxSuccessful());
  }

  @SneakyThrows
  protected static ResultActions tryPost(String uri, Object body, Object... args) {
    return tryPost(uri, body, defaultHeaders(), args);
  }

  @SneakyThrows
  protected static ResultActions tryPost(String uri, Object body, HttpHeaders headers, Object... args) {
    return tryDoHttpMethod(post(uri, args), body, headers);
  }

  @SneakyThrows
  protected static ResultActions doPost(String uri, Object body, Object... args) {
    return doPost(uri, body, defaultHeaders(), args);
  }

  @SneakyThrows
  protected static ResultActions doPost(String uri, Object body, HttpHeaders headers, Object... args) {
    return tryPost(uri, body, headers, args).andExpect(status().is2xxSuccessful());
  }

  @SneakyThrows
  protected static void sendKafkaMessage(String topic, String key, Object event) {
    var future = kafkaTemplate.send(topic, key, new ObjectMapper().writeValueAsString(event));
    awaitUntilAsserted(() -> Assertions.assertTrue(future.isDone(), "Message was not sent"));
  }

  protected static void awaitUntilAsserted(ThrowingRunnable throwingRunnable) {
    await().pollInterval(ONE_SECOND).atMost(TEN_SECONDS).untilAsserted(throwingRunnable);
  }

  protected <T> void verifyReceivedDomainEvent(ConsumerRecord<String, DomainEvent> receivedEvent,
                                               DomainEventType expectedEventType,
                                               List<String> expectedHeaderKeys,
                                               T expectedDto,
                                               Class<T> dtoClassType,
                                               String... ignoreFields) throws JsonProcessingException {
    assertNotNull(receivedEvent);
    var headerKeys = Arrays.stream(receivedEvent.headers().toArray())
      .map(Header::key)
      .collect(Collectors.toSet());
    var domainType = Arrays.stream(receivedEvent.headers().toArray())
      .filter(header -> header.key().equals(DOMAIN_EVENT_HEADER_KEY))
      .map(Header::value)
      .map(String::new)
      .findFirst().orElse("");

    assertThat(headerKeys).containsAll(expectedHeaderKeys);
    assertThat(domainType).isEqualTo(expectedEventType.toString());

    assertNotNull(receivedEvent.value());
    var event = (DomainEvent<T>) receivedEvent.value();
    var eventDtoAsString = "";
    if (List.of(DomainEventType.CREATE, DomainEventType.UPDATE, DomainEventType.REINDEX).contains(expectedEventType)) {
      eventDtoAsString = objectMapper.writeValueAsString(event.getNewEntity());
    } else if (expectedEventType == DomainEventType.DELETE) {
      eventDtoAsString = objectMapper.writeValueAsString(event.getOldEntity());
    }
    var dtoFromEvent = objectMapper.readValue(eventDtoAsString, dtoClassType);

    var comparison = assertThat(dtoFromEvent).usingRecursiveComparison();
    if (ignoreFields.length > 0) {
      comparison = comparison.ignoringFields(ignoreFields);
    }
    comparison.isEqualTo(expectedDto);
  }

  protected <T> ResultMatcher exceptionMatch(Class<T> type) {
    return result -> MatcherAssert.assertThat(result.getResolvedException(), instanceOf(type));
  }

  @BeforeAll
  static void setUp(@Autowired MockMvc mockMvc,
                    @Autowired ObjectMapper objectMapper,
                    @Autowired KafkaTemplate<String, String> kafkaTemplate,
                    @Autowired DatabaseHelper databaseHelper) {
    System.setProperty("env", "folio-test");
    IntegrationTestBase.mockMvc = mockMvc;
    IntegrationTestBase.objectMapper = objectMapper;
    IntegrationTestBase.kafkaTemplate = kafkaTemplate;
    IntegrationTestBase.databaseHelper = databaseHelper;
  }

  @AfterAll
  static void afterAll() {
    System.clearProperty("env");
  }

  @BeforeEach
  void beforeEach(@Autowired CacheManager cacheManager) {
    cacheManager.getCacheNames().forEach(name -> requireNonNull(cacheManager.getCache(name)).clear());
  }

  @NotNull
  private static ResultActions tryDoHttpMethod(MockHttpServletRequestBuilder builder, Object body,
                                               HttpHeaders headers) throws Exception {
    return mockMvc.perform(builder
        .content(body == null ? "" : asJson(body, objectMapper))
        .headers(headers))
      .andDo(log());
  }

  @NotNull
  private static ResultActions tryDoHttpMethod(MockHttpServletRequestBuilder builder, Object body) throws Exception {
    return tryDoHttpMethod(builder, body, defaultHeaders());
  }

  @TestConfiguration
  public static class KafkaTemplateTestConfiguration {

    @Bean
    @Primary
    public ProducerFactory<String, String> producerStringFactory(KafkaProperties kafkaProperties) {
      Map<String, Object> configProps = new HashMap<>(kafkaProperties.buildProducerProperties());
      configProps.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      configProps.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    @Primary
    public KafkaTemplate<String, String> kafkaStringTemplate(ProducerFactory<String, String> producerFactory) {
      return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public DatabaseHelper databaseHelper(JdbcTemplate jdbcTemplate, FolioModuleMetadata moduleMetadata) {
      return new DatabaseHelper(moduleMetadata, jdbcTemplate);
    }
  }
}
