package org.folio.entlinks.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.folio.entlinks.domain.dto.ReindexJobDto.JobStatusEnum.IDS_PUBLISHED;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.MatchUtils.errorMessageMatch;
import static org.folio.support.TestDataUtils.AuthorityTestData.authorityDto;
import static org.folio.support.TestDataUtils.AuthorityTestData.authorityReindexJob;
import static org.folio.support.TestDataUtils.AuthorityTestData.authoritySourceFile;
import static org.folio.support.TestDataUtils.AuthorityTestData.authoritySourceFileCode;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.authorityReindexEndpoint;
import static org.folio.support.base.TestConstants.authorityTopic;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.controller.converter.ReindexJobMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityDtoCollection;
import org.folio.entlinks.domain.dto.ReindexJobDto;
import org.folio.entlinks.domain.dto.ReindexJobDtoCollection;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.entlinks.service.reindex.event.DomainEvent;
import org.folio.entlinks.service.reindex.event.DomainEventType;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.test.extension.DatabaseCleanup;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;


@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.AUTHORITY_REINDEX_JOB_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE,
  DatabaseHelper.AUTHORITY_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE})
class AuthorityReindexControllerIT extends IntegrationTestBase {

  private static final List<String> DOMAIN_EVENT_HEADER_KEYS =
      List.of(XOkapiHeaders.TENANT, XOkapiHeaders.URL, XOkapiHeaders.USER_ID, DOMAIN_EVENT_HEADER_KEY);

  @Autowired
  ReindexJobMapper mapper;

  private KafkaMessageListenerContainer<String, DomainEvent> container;
  private BlockingQueue<ConsumerRecord<String, DomainEvent>> consumerRecords;

  @BeforeEach
  void setUp(@Autowired KafkaProperties kafkaProperties) {
    consumerRecords = new LinkedBlockingQueue<>();
    container = createAndStartTestConsumer(authorityTopic(), consumerRecords, kafkaProperties, DomainEvent.class);
  }

  @AfterEach
  void tearDown() {
    consumerRecords.clear();
    container.stop();
  }

  @Test
  @DisplayName("Get Collection: find no ReindexJob")
  void getCollection_positive_noEntitiesFound() throws Exception {
    doGet(authorityReindexEndpoint())
        .andExpect(jsonPath("totalRecords", is(0)));
  }

  @Test
  @DisplayName("Get Collection: find all ReindexJobs")
  void getCollection_positive_entitiesFound() throws Exception {
    var reindexJob = createReindexJob();
    var dto = mapper.toDto(reindexJob);

    var contentAsString = doGet(authorityReindexEndpoint())
        .andReturn().getResponse().getContentAsString();
    var resultCollectionDto = objectMapper.readValue(contentAsString, ReindexJobDtoCollection.class);

    assertThat(resultCollectionDto.getTotalRecords()).isEqualTo(1);
    assertThat(resultCollectionDto.getReindexJobs().get(0).getSubmittedDate()).isNotNull();
    assertThat(resultCollectionDto.getReindexJobs())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("submittedDate")
        .isEqualTo(List.of(dto));
  }

  @Test
  @DisplayName("Get By ID: return Reindex Job by given ID")
  void getById_positive_foundByIdForExistingEntity() throws Exception {
    var reindexJob = createReindexJob();
    var dto = mapper.toDto(reindexJob);

    var contentAsString = doGet(authorityReindexEndpoint(reindexJob.getId()))
        .andReturn().getResponse().getContentAsString();
    var resultDto = objectMapper.readValue(contentAsString, ReindexJobDto.class);

    assertThat(resultDto.getSubmittedDate()).isNotNull();
    assertThat(resultDto)
        .usingRecursiveComparison()
        .ignoringFields("submittedDate")
        .isEqualTo(dto);
  }

  @Test
  @DisplayName("Submit Reindex Job and Publish Events")
  void submitReindexJob_positive_shouldSubmitJobAndPublishReindexEventForAllAuthorities() throws Exception {
    assumeTrue(databaseHelper.countRows(DatabaseHelper.AUTHORITY_REINDEX_JOB_TABLE, TENANT_ID) == 0);
    var dtos = createAuthorityData(false);
    assumeTrue(databaseHelper.countRows(DatabaseHelper.AUTHORITY_TABLE, TENANT_ID) == 3);

    tryPost(authorityReindexEndpoint(), null).andExpect(status().isCreated());

    var receivedEvents = List.of(getReceivedEvent(), getReceivedEvent(), getReceivedEvent());
    verifyReceivedEvents(receivedEvents, dtos);
    assertEquals(1, databaseHelper.countRows(DatabaseHelper.AUTHORITY_REINDEX_JOB_TABLE, TENANT_ID));
    awaitUntilAsserted(() ->
            doGet(authorityReindexEndpoint())
                .andExpect(jsonPath("reindexJobs[0].jobStatus", is(IDS_PUBLISHED.getValue())))
    );
  }

  @Test
  @DisplayName("Cancel submitted in-progress reindex job")
  void cancelReindexJob_positive_shouldCancelSubmittedReindexJob()
      throws UnsupportedEncodingException, JsonProcessingException {
    createAuthoritySourceFile();
    var authority = TestDataUtils.AuthorityTestData.authority(0, 0);
    for (int i = 0; i < 1000; i++) {
      authority.setId(UUID.randomUUID());
      databaseHelper.saveAuthority(TENANT_ID, authority);
    }
    doPost(authorityReindexEndpoint(), null);

    var content = doGet(authorityReindexEndpoint())
        .andReturn().getResponse().getContentAsString();
    var dto = objectMapper.readValue(content, ReindexJobDtoCollection.class);
    var id = dto.getReindexJobs().iterator().next().getId();

    doDelete(authorityReindexEndpoint(id));

    // wait for processing the authorities to finish - we expect that cancelled job would not change it's status as
    // reindex event publishing thread eventually detect that job is cancelled (when logging the progress) and stop
    // the event publishing. This can only be simulated when we have at least 1000 authorities as we log progress at
    // every 1000 authorities processing
    await().timeout(TEN_SECONDS);

    var cancelledContent = doGet(authorityReindexEndpoint(id))
        .andReturn().getResponse().getContentAsString();
    var cancelled = objectMapper.readValue(cancelledContent, ReindexJobDto.class);
    assertThat(cancelled.getJobStatus().getValue()).isEqualTo(ReindexJobDto.JobStatusEnum.PENDING_CANCEL.getValue());
  }

  @Test
  @DisplayName("Cancelling published job should result with error")
  void cancelReindexJob_negative_notPossibleToCancelJobWithIdsPublishedStatus() throws Exception {
    createAuthoritySourceFile();
    createAuthority(false);
    doPost(authorityReindexEndpoint(), null);
    awaitUntilAsserted(() ->
      doGet(authorityReindexEndpoint())
          .andExpect(jsonPath("reindexJobs[0].jobStatus", is(IDS_PUBLISHED.getValue())))
    );

    var content = doGet(authorityReindexEndpoint())
        .andReturn().getResponse().getContentAsString();
    var dto = objectMapper.readValue(content, ReindexJobDtoCollection.class);
    var id = dto.getReindexJobs().iterator().next().getId();

    tryDelete(authorityReindexEndpoint(id))
        .andExpect(status().isInternalServerError())
        .andExpect(exceptionMatch(IllegalStateException.class))
        .andExpect(errorMessageMatch(containsString("The job has been finished")));
  }

  private void verifyReceivedEvents(List<ConsumerRecord<String, DomainEvent>> receivedEvents, List<AuthorityDto> dtos)
      throws JsonProcessingException {
    for (var receivedEvent : receivedEvents) {
      var expectedDto = dtos.stream()
          .filter(dto -> dto.getId().toString().equals(receivedEvent.key()))
          .findFirst().get();
      verifyReceivedDomainEvent(receivedEvent, DomainEventType.REINDEX, DOMAIN_EVENT_HEADER_KEYS,
          expectedDto, AuthorityDto.class, "metadata.createdDate", "metadata.updatedDate");
    }
  }

  private ReindexJob createReindexJob() {
    var entity = authorityReindexJob();
    databaseHelper.saveAuthorityReindexJob(TENANT_ID, entity);
    return entity;
  }

  private List<AuthorityDto> createAuthorityData(boolean autogenerateId)
      throws UnsupportedEncodingException, JsonProcessingException {
    createAuthoritySourceFile();
    return createAuthority(autogenerateId);
  }

  private void createAuthoritySourceFile() {
    var sourceFile = authoritySourceFile(0);
    var sourceFileCode = authoritySourceFileCode(0);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, sourceFile);
    databaseHelper.saveAuthoritySourceFileCode(TENANT_ID, sourceFile.getId(), sourceFileCode);
  }

  private List<AuthorityDto> createAuthority(boolean autogenerateId)
      throws JsonProcessingException, UnsupportedEncodingException {
    var authority1 = authorityDto(0, 0);
    var authority2 = authorityDto(1, 0);
    var authority3 = authorityDto(2, 0);
    if (autogenerateId) {
      authority1.setId(null);
      authority2.setId(null);
      authority3.setId(null);
    }
    doPost(authorityEndpoint(), authority1);
    doPost(authorityEndpoint(), authority2);
    doPost(authorityEndpoint(), authority3);
    clearReceivedEvents(3);

    var authorityCollectionContent = doGet(authorityEndpoint()).andReturn().getResponse().getContentAsString();
    var collectionDto = objectMapper.readValue(authorityCollectionContent, AuthorityDtoCollection.class);
    return collectionDto.getAuthorities();
  }

  @Nullable
  @SneakyThrows
  private ConsumerRecord<String, DomainEvent> getReceivedEvent() {
    return consumerRecords.poll(10, TimeUnit.SECONDS);
  }

  private void clearReceivedEvents(int num) {
    for (int i = 0; i < num; i++) {
      getReceivedEvent();
    }
  }
}
