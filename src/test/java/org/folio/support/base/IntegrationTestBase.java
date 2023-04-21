package org.folio.support.base;

import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.folio.support.JsonTestUtils.asJson;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.core.ThrowingRunnable;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.test.extension.EnableKafka;
import org.folio.spring.test.extension.EnableOkapi;
import org.folio.spring.test.extension.EnablePostgres;
import org.folio.spring.test.extension.impl.OkapiConfiguration;
import org.folio.support.DatabaseHelper;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@EnableKafka
@EnableOkapi
@EnablePostgres
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(IntegrationTestBase.KafkaTemplateTestConfiguration.class)
public class IntegrationTestBase {

  protected static MockMvc mockMvc;
  protected static OkapiConfiguration okapi;
  protected static KafkaTemplate<String, String> kafkaTemplate;
  protected static ObjectMapper objectMapper;
  protected static DatabaseHelper databaseHelper;

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
    setUpTenant();
  }

  @AfterAll
  static void tearDown() {
    System.clearProperty("env");
  }

  @SneakyThrows
  protected static void setUpTenant() {
    doPost("/_/tenant", new TenantAttributes().moduleTo("mod-entities-links"));
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
    return mockMvc.perform(rb
        .headers(defaultHeaders()).accept(APPLICATION_JSON))
      .andDo(log());
  }

  @SneakyThrows
  protected static ResultActions tryGet(String uri, Object... args) {
    return mockMvc.perform(get(uri, args)
        .headers(defaultHeaders()).accept(APPLICATION_JSON))
      .andDo(log());
  }

  @SneakyThrows
  protected static ResultActions doGet(String uri, Object... args) {
    return tryGet(uri, args).andExpect(status().isOk());
  }

  @SneakyThrows
  protected static ResultActions tryPut(String uri, Object body, Object... args) {
    return tryDoHttpMethod(put(uri, args), body);
  }

  @SneakyThrows
  protected static ResultActions doPut(String uri, Object body, Object... args) {
    return tryPut(uri, body, args).andExpect(status().is2xxSuccessful());
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
    return tryDoHttpMethod(post(uri, args), body);
  }

  @NotNull
  private static ResultActions tryDoHttpMethod(MockHttpServletRequestBuilder builder, Object body) throws Exception {
    return mockMvc.perform(builder
        .content(body == null ? "" : asJson(body, objectMapper))
        .headers(defaultHeaders()))
      .andDo(log());
  }

  @SneakyThrows
  protected static ResultActions doPost(String uri, Object body, Object... args) {
    return tryPost(uri, body, args).andExpect(status().is2xxSuccessful());
  }

  @SneakyThrows
  protected static void sendKafkaMessage(String topic, String key, Object event) {
    var future = kafkaTemplate.send(topic, key, new ObjectMapper().writeValueAsString(event));
    awaitUntilAsserted(() -> Assertions.assertTrue(future.isDone(), "Message was not sent"));
  }

  protected static void awaitUntilAsserted(ThrowingRunnable throwingRunnable) {
    await().pollInterval(ONE_SECOND).atMost(TEN_SECONDS).untilAsserted(throwingRunnable);
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
