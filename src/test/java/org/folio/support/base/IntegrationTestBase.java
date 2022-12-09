package org.folio.support.base;

import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.folio.support.TestUtils.asJson;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.kafka.common.serialization.StringSerializer;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.test.extension.EnableKafka;
import org.folio.spring.test.extension.EnableOkapi;
import org.folio.spring.test.extension.EnablePostgres;
import org.folio.spring.test.extension.impl.OkapiConfiguration;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.hamcrest.Matcher;
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
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

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

  @BeforeAll
  static void setUp(@Autowired MockMvc mockMvc,
                    @Autowired ObjectMapper objectMapper,
                    @Autowired KafkaTemplate<String, String> kafkaTemplate) {
    System.setProperty("env", "folio-test");
    IntegrationTestBase.mockMvc = mockMvc;
    IntegrationTestBase.objectMapper = objectMapper;
    IntegrationTestBase.kafkaTemplate = kafkaTemplate;
    setUpTenant();
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

  protected static WireMockServer getWireMock() {
    return okapi.wireMockServer();
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
    return mockMvc.perform(put(uri, args)
        .content(body == null ? "" : asJson(body))
        .headers(defaultHeaders()))
      .andDo(log());
  }

  @SneakyThrows
  protected static ResultActions doPut(String uri, Object body, Object... args) {
    return tryPut(uri, body, args).andExpect(status().is2xxSuccessful());
  }

  @SneakyThrows
  protected static ResultActions tryPost(String uri, Object body, Object... args) {
    return mockMvc.perform(post(uri, args)
        .content(asJson(body))
        .headers(defaultHeaders()))
      .andDo(log());
  }

  @SneakyThrows
  protected static ResultActions doPost(String uri, Object body, Object... args) {
    return tryPost(uri, body, args).andExpect(status().is2xxSuccessful());
  }

  @SneakyThrows
  protected static void sendKafkaMessage(String topic, Object event) {
    kafkaTemplate.send(topic, new ObjectMapper().writeValueAsString(event));
  }

  @SneakyThrows
  protected static void sendKafkaMessage(String topic, String key, Object event) {
    kafkaTemplate.send(topic, key, new ObjectMapper().writeValueAsString(event));
  }

  protected ResultMatcher errorParameterMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].parameters.[0].key", errorMessageMatcher);
  }

  protected ResultMatcher errorTypeMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].type", errorMessageMatcher);
  }

  protected ResultMatcher errorCodeMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].code", errorMessageMatcher);
  }

  protected ResultMatcher errorMessageMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].message", errorMessageMatcher);
  }

  protected ResultMatcher errorTotalMatch(int errorTotal) {
    return jsonPath("$.total_records", is(errorTotal));
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
  }
}
