package org.folio.entlinks.controller;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.folio.support.TestUtils.Link.TAGS;
import static org.folio.support.TestUtils.linksDto;
import static org.folio.support.TestUtils.linksDtoCollection;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.folio.entlinks.model.type.ErrorCode;
import org.folio.qm.domain.dto.InstanceLinkDto;
import org.folio.qm.domain.dto.InstanceLinkDtoCollection;
import org.folio.qm.domain.dto.UuidCollection;
import org.folio.support.TestUtils.Link;
import org.folio.support.base.IntegrationTestBase;
import org.folio.support.types.IntegrationTest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.ResultMatcher;

@IntegrationTest
class InstanceLinksIT extends IntegrationTestBase {

  private static final String INSTANCE_LINKS_ENDPOINT_PATH = "/links/instances/{id}";
  private static final String AUTHORITY_LINKS_COUNT_ENDPOINT_PATH = "/links/authorities/bulk/count";

  public static Stream<Arguments> requiredFieldMissingProvider() {
    return Stream.of(
      arguments("instanceId",
        new InstanceLinkDto()
          .authorityId(randomUUID()).authorityNaturalId("id")
          .bibRecordTag("100").bibRecordSubfields(List.of("a"))
      ),
      arguments("authorityId",
        new InstanceLinkDto().instanceId(randomUUID())
          .authorityNaturalId("id")
          .bibRecordTag("100").bibRecordSubfields(List.of("a"))
      ),
      arguments("authorityNaturalId",
        new InstanceLinkDto().instanceId(randomUUID())
          .authorityId(randomUUID())
          .bibRecordTag("100").bibRecordSubfields(List.of("a"))
      ),
      arguments("bibRecordTag",
        new InstanceLinkDto().instanceId(randomUUID())
          .authorityId(randomUUID()).authorityNaturalId("id")
          .bibRecordSubfields(List.of("a"))
      )
    );
  }

  @Test
  void getInstanceLinks_positive_noLinksFound() throws Exception {
    doGet(INSTANCE_LINKS_ENDPOINT_PATH, randomUUID())
      .andExpect(linksMatch(empty()))
      .andExpect(totalRecordsMatch(0));
  }

  @Test
  void getInstanceLinks_negative_invalidId() throws Exception {
    tryGet(INSTANCE_LINKS_ENDPOINT_PATH, "not a uuid")
      .andExpect(status().isBadRequest())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("MethodArgumentTypeMismatchException")))
      .andExpect(errorCodeMatch(is(ErrorCode.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("Invalid UUID string")));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_saveIncomingLinks_whenAnyExist() {
    var instanceId = randomUUID();
    var incomingLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 0), Link.of(1, 1)));
    doPut(INSTANCE_LINKS_ENDPOINT_PATH, incomingLinks, instanceId);

    doGet(INSTANCE_LINKS_ENDPOINT_PATH, instanceId)
      .andExpect(linksMatch(hasSize(2)))
      .andExpect(linksMatch(incomingLinks))
      .andExpect(totalRecordsMatch(2));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_deleteAllLinks_whenIncomingIsEmpty() {
    var instanceId = randomUUID();
    var existedLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 0), Link.of(1, 1)));
    doPut(INSTANCE_LINKS_ENDPOINT_PATH, existedLinks, instanceId);

    var incomingLinks = linksDtoCollection(emptyList());
    doPut(INSTANCE_LINKS_ENDPOINT_PATH, incomingLinks, instanceId);

    doGet(INSTANCE_LINKS_ENDPOINT_PATH, instanceId)
      .andExpect(linksMatch(hasSize(0)))
      .andExpect(totalRecordsMatch(0));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_deleteAllExistedAndSaveAllIncomingLinks() {
    var instanceId = randomUUID();
    var existedLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    ));
    doPut(INSTANCE_LINKS_ENDPOINT_PATH, existedLinks, instanceId);

    var incomingLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 1),
      Link.of(1, 0),
      Link.of(2, 3),
      Link.of(3, 2)
    ));
    doPut(INSTANCE_LINKS_ENDPOINT_PATH, incomingLinks, instanceId);

    doGet(INSTANCE_LINKS_ENDPOINT_PATH, instanceId)
      .andExpect(linksMatch(hasSize(4)))
      .andExpect(linksMatch(incomingLinks))
      .andExpect(totalRecordsMatch(4));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_saveOnlyNewLinks() {
    var instanceId = randomUUID();
    var existedLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 0),
      Link.of(1, 1)
    ));
    doPut(INSTANCE_LINKS_ENDPOINT_PATH, existedLinks, instanceId);

    var incomingLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    ));
    doPut(INSTANCE_LINKS_ENDPOINT_PATH, incomingLinks, instanceId);

    doGet(INSTANCE_LINKS_ENDPOINT_PATH, instanceId)
      .andExpect(linksMatch(hasSize(4)))
      .andExpect(linksMatch(incomingLinks))
      .andExpect(totalRecordsMatch(4));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_deleteAndSaveLinks_whenHaveDifference() {
    var instanceId = randomUUID();
    var existedLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    ));
    doPut(INSTANCE_LINKS_ENDPOINT_PATH, existedLinks, instanceId);

    var incomingLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    ));
    doPut(INSTANCE_LINKS_ENDPOINT_PATH, incomingLinks, instanceId);

    doGet(INSTANCE_LINKS_ENDPOINT_PATH, instanceId)
      .andExpect(linksMatch(hasSize(4)))
      .andExpect(linksMatch(incomingLinks))
      .andExpect(totalRecordsMatch(4));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_negative_whenInstanceIdIsNotSameForIncomingLinks() {
    var instanceId = randomUUID();
    var incomingLinks = linksDtoCollection(linksDto(randomUUID(),
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    ));

    tryPut(INSTANCE_LINKS_ENDPOINT_PATH, incomingLinks, instanceId)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("RequestBodyValidationException")))
      .andExpect(errorCodeMatch(is(ErrorCode.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("Link should have instanceId = " + instanceId)));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_negative_whenInstanceIdIsNotUuid() {
    var invalidInstanceId = "1111";
    var incomingLinks = linksDtoCollection(emptyList());

    tryPut(INSTANCE_LINKS_ENDPOINT_PATH, incomingLinks, invalidInstanceId)
      .andExpect(status().isBadRequest())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("MethodArgumentTypeMismatchException")))
      .andExpect(errorCodeMatch(is(ErrorCode.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("Invalid UUID string")));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_negative_whenBodyIsEmpty() {
    var instanceId = randomUUID();

    tryPut(INSTANCE_LINKS_ENDPOINT_PATH, null, instanceId)
      .andExpect(status().isBadRequest())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("HttpMessageNotReadableException")))
      .andExpect(errorCodeMatch(is(ErrorCode.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("Required request body is missing")));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_negative_whenBibRecordSubfieldsIsEmpty() {
    var instanceId = randomUUID();

    var incomingLinks = linksDtoCollection(List.of(new InstanceLinkDto()
      .instanceId(randomUUID()).authorityId(randomUUID())
      .authorityNaturalId("id").bibRecordTag("100")
    ));

    tryPut(INSTANCE_LINKS_ENDPOINT_PATH, incomingLinks, instanceId)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("MethodArgumentNotValidException")))
      .andExpect(errorCodeMatch(is(ErrorCode.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("size must be between 1 and 100")))
      .andExpect(errorParameterMatch(is("links[0].bibRecordSubfields")));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_negative_whenBibRecordSubfieldsValuesHaveMoreThenOneChar() {
    var instanceId = randomUUID();

    var incomingLinks = linksDtoCollection(List.of(new InstanceLinkDto()
      .instanceId(instanceId).authorityId(randomUUID())
      .authorityNaturalId("id").bibRecordTag("100")
      .bibRecordSubfields(List.of("aa", "bb", "11"))
    ));

    tryPut(INSTANCE_LINKS_ENDPOINT_PATH, incomingLinks, instanceId)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("RequestBodyValidationException")))
      .andExpect(errorCodeMatch(is(ErrorCode.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("Max Bib record subfield length is 1")))
      .andExpect(errorParameterMatch(is("bibRecordSubfields")));
  }

  @SneakyThrows
  @MethodSource("requiredFieldMissingProvider")
  @ParameterizedTest(name = "[{index}] missing {0}")
  void updateInstanceLinks_negative_whenRequiredFieldIsMissing(String missingField, InstanceLinkDto invalidLink) {
    var instanceId = randomUUID();
    var incomingLinks = linksDtoCollection(List.of(invalidLink));

    tryPut(INSTANCE_LINKS_ENDPOINT_PATH, incomingLinks, instanceId)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("MethodArgumentNotValidException")))
      .andExpect(errorCodeMatch(is(ErrorCode.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("must not be null")))
      .andExpect(errorParameterMatch(is("links[0]." + missingField)));
  }

  @Test
  @SneakyThrows
  void countNumberOfTitles_positive_whenInstanceLinksExist() {
    var instanceId = randomUUID();
    var authorityId = randomUUID();
    var links = linksDtoCollection(linksDto(instanceId,
        new Link(authorityId, TAGS[0]),
        new Link(authorityId, TAGS[1]),
        new Link(authorityId, TAGS[2])
    ));
    doPut(INSTANCE_LINKS_ENDPOINT_PATH, links, instanceId);

    var requestBody = new UuidCollection().ids(List.of(authorityId));
    doPost(AUTHORITY_LINKS_COUNT_ENDPOINT_PATH, requestBody)
        .andExpect(status().isOk())
        .andExpect(linksMatch(hasSize(1)))
        .andExpect(jsonPath("$.links.[0].id", is(authorityId.toString())))
        .andExpect(jsonPath("$.links.[0].totalLinks", is(3)));
  }

  @Test
  @SneakyThrows
  void countNumberOfTitles_positive_whenInstanceLinksNotExistThenReturnZeroCount() {
    var requestBody = new UuidCollection().ids(List.of(randomUUID(), randomUUID()));
    doPost(AUTHORITY_LINKS_COUNT_ENDPOINT_PATH, requestBody)
        .andExpect(status().isOk())
        .andExpect(linksMatch(hasSize(2)))
        .andExpect(jsonPath("$.links.[0].totalLinks", is(0)))
        .andExpect(jsonPath("$.links.[1].totalLinks", is(0)));
  }

  @Test
  @SneakyThrows
  void countNumberOfTitles_positive_whenRequestBodyIsEmptyThenReturnEmptyList() {
    var requestBody = new UuidCollection().ids(List.of());
    doPost(AUTHORITY_LINKS_COUNT_ENDPOINT_PATH, requestBody)
        .andExpect(status().isOk())
        .andExpect(linksMatch(hasSize(0)));
  }

  @Test
  @SneakyThrows
  void countNumberOfTitles_negative_whenRequestBodyInvalidThenThrowsValidationException() {
    var requestBody = List.of("not uuid collection object");
    tryPost(AUTHORITY_LINKS_COUNT_ENDPOINT_PATH, requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(errorTotalMatch(1))
        .andExpect(errorTypeMatch(is("HttpMessageNotReadableException")))
        .andExpect(errorCodeMatch(is(ErrorCode.VALIDATION_ERROR.getValue())));
  }

  private ResultMatcher errorParameterMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].parameters.[0].key", errorMessageMatcher);
  }

  private ResultMatcher errorTypeMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].type", errorMessageMatcher);
  }

  private ResultMatcher errorCodeMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].code", errorMessageMatcher);
  }

  private ResultMatcher errorMessageMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].message", errorMessageMatcher);
  }

  private ResultMatcher errorTotalMatch(int errorTotal) {
    return jsonPath("$.total_records", is(errorTotal));
  }

  private ResultMatcher totalRecordsMatch(int recordsTotal) {
    return jsonPath("$.totalRecords", is(recordsTotal));
  }

  private ResultMatcher linksMatch(Matcher<Collection<? extends InstanceLinkDto>> matcher) {
    return jsonPath("$.links", matcher);
  }

  @SuppressWarnings("unchecked")
  private ResultMatcher linksMatch(InstanceLinkDtoCollection links) {
    var linkMatchers = links.getLinks().stream()
      .map(LinkMatcher::linkMatch)
      .toArray(Matcher[]::new);
    return jsonPath("$.links", containsInAnyOrder(linkMatchers));
  }

  private static class LinkMatcher extends BaseMatcher<InstanceLinkDto> {

    private final InstanceLinkDto expectedLink;

    private LinkMatcher(InstanceLinkDto expectedLink) { this.expectedLink = expectedLink; }

    static LinkMatcher linkMatch(InstanceLinkDto expectedLink) {
      return new LinkMatcher(expectedLink);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean matches(Object actual) {
      if (actual instanceof LinkedHashMap actualLink) {
        return Objects.equals(expectedLink.getAuthorityId().toString(), actualLink.get("authorityId")) &&
          Objects.equals(expectedLink.getAuthorityNaturalId(), actualLink.get("authorityNaturalId")) &&
          Objects.equals(expectedLink.getInstanceId().toString(), actualLink.get("instanceId")) &&
          Objects.equals(expectedLink.getBibRecordTag(), actualLink.get("bibRecordTag")) &&
          Objects.equals(expectedLink.getBibRecordSubfields(), actualLink.get("bibRecordSubfields"));
      }

      return false;
    }

    @Override
    public void describeTo(Description description) {
      description.appendValue(expectedLink);
    }
  }

}
