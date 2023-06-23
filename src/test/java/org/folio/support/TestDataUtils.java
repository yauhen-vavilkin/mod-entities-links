package org.folio.support;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus.ACTUAL;
import static org.folio.entlinks.utils.DateUtils.fromTimestamp;
import static org.folio.support.base.TestConstants.TENANT_ID;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.folio.entlinks.domain.dto.AuthorityControlMetadata;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.AuthorityStatsDto;
import org.folio.entlinks.domain.dto.BibStatsDto;
import org.folio.entlinks.domain.dto.BibStatsDtoCollection;
import org.folio.entlinks.domain.dto.ExternalIdsHolder;
import org.folio.entlinks.domain.dto.FieldContent;
import org.folio.entlinks.domain.dto.InstanceLinkDto;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.dto.LinkAction;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.dto.ParsedRecordContent;
import org.folio.entlinks.domain.dto.RecordType;
import org.folio.entlinks.domain.dto.StrippedParsedRecord;
import org.folio.entlinks.domain.dto.StrippedParsedRecordCollection;
import org.folio.entlinks.domain.dto.StrippedParsedRecordParsedRecord;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.folio.entlinks.domain.entity.AuthorityDataStatStatus;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.spring.tools.client.UsersClient;
import org.folio.spring.tools.model.ResultList;

@UtilityClass
public class TestDataUtils {

  public static InventoryEvent inventoryEvent(String resource, String type,
                                              AuthorityInventoryRecord n, AuthorityInventoryRecord o) {
    return new InventoryEvent().type(type).resourceName(resource).tenant(TENANT_ID)._new(n).old(o);
  }

  public static InventoryEvent authorityEvent(String type, AuthorityInventoryRecord n, AuthorityInventoryRecord o) {
    return inventoryEvent("authority", type, n, o);
  }

  public static List<InstanceLinkDto> linksDto(UUID instanceId, Link... links) {
    return Arrays.stream(links).map(link -> link.toDto(instanceId)).toList();
  }

  public static InstanceLinkDtoCollection linksDtoCollection(List<InstanceLinkDto> links) {
    return new InstanceLinkDtoCollection(links);
  }

  public static List<InstanceAuthorityLink> links(UUID instanceId, Link... links) {
    return Arrays.stream(links).map(link -> link.toEntity(instanceId)).toList();
  }

  public static List<InstanceAuthorityLink> links(List<Integer> ids) {
    return ids.stream()
      .map(id -> InstanceAuthorityLink.builder()
        .id((long) id)
        .build())
      .toList();
  }

  public static List<InstanceAuthorityLink> links(int count, String error) {
    return Stream.generate(() -> 0)
      .map(i -> {
        var link = InstanceAuthorityLink.builder()
          .id((long) RandomUtils.nextInt())
          .instanceId(UUID.randomUUID())
          .linkingRule(InstanceAuthorityLinkingRule.builder()
            .bibField("100")
            .build())
          .authorityData(AuthorityData.builder()
            .naturalId("naturalId")
            .build())
          .errorCause(error)
          .build();
        link.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return link;
      })
      .limit(count)
      .toList();
  }

  public static List<InstanceAuthorityLink> links(int count) {
    return links(count, EMPTY);
  }

  public static List<LinkUpdateReport> reports(UUID jobId) {
    return reports(jobId, LinkUpdateReport.StatusEnum.SUCCESS, EMPTY);
  }

  public static List<LinkUpdateReport> reports(UUID jobId, LinkUpdateReport.StatusEnum status, String failCause) {
    var tenant = RandomStringUtils.randomAlphabetic(10);
    return List.of(
      report(tenant, jobId, status, failCause),
      report(tenant, jobId, status, failCause));
  }

  public static LinkUpdateReport report(String tenant, UUID jobId) {
    return report(tenant, jobId, LinkUpdateReport.StatusEnum.SUCCESS, EMPTY);
  }

  public static LinkUpdateReport report(String tenant, UUID jobId, LinkUpdateReport.StatusEnum status,
                                        String failCause) {
    return new LinkUpdateReport()
      .tenant(tenant)
      .jobId(jobId)
      .instanceId(UUID.randomUUID())
      .status(status)
      .linkIds(List.of(RandomUtils.nextInt(), RandomUtils.nextInt()))
      .failCause(failCause);
  }

  public static List<BibStatsDto> stats(List<InstanceAuthorityLink> links) {
    return links.stream()
      .map(link -> new BibStatsDto()
        .instanceId(link.getInstanceId())
        .bibRecordTag(link.getLinkingRule().getBibField())
        .authorityNaturalId(link.getAuthorityData().getNaturalId())
        .updatedAt(fromTimestamp(link.getUpdatedAt()))
        .errorCause(link.getErrorCause()))
      .toList();
  }

  public static BibStatsDtoCollection stats(List<InstanceLinkDto> links, String errorCause, OffsetDateTime next,
                                            String instanceTitle) {
    var stats = links.stream()
      .map(link -> new BibStatsDto()
        .instanceId(link.getInstanceId())
        .bibRecordTag(Link.RULE_IDS_TO_TAGS.get(link.getLinkingRuleId()))
        .authorityNaturalId(link.getAuthorityNaturalId())
        .instanceTitle(instanceTitle)
        .updatedAt(OffsetDateTime.now())
        .errorCause(errorCause))
      .collect(Collectors.toList());
    // because returned by updatedDate desc
    Collections.reverse(stats);

    return new BibStatsDtoCollection()
      .stats(stats)
      .next(next);
  }

  public static AuthorityDataStat authorityDataStat(UUID userId, UUID sourceFileId, AuthorityDataStatAction action) {
    return AuthorityDataStat.builder()
      .id(randomUUID())
      .action(action)
      .authorityData(AuthorityData.builder()
        .id(UUID.randomUUID())
        .naturalId("naturalIdNew")
        .build())
      .authorityNaturalIdOld("naturalIdOld")
      .authorityNaturalIdNew("naturalIdNew")
      .authoritySourceFileNew(sourceFileId)
      .authoritySourceFileOld(UUID.randomUUID())
      .completedAt(Timestamp.from(Instant.now()))
      .headingNew("headingNew")
      .headingOld("headingOld")
      .headingTypeNew("headingTypeNew")
      .headingTypeOld("headingTypeOld")
      .lbUpdated(2)
      .lbFailed(1)
      .lbTotal(5)
      .startedAt(Timestamp.from(Instant.now().minus(4, ChronoUnit.DAYS)))
      .startedByUserId(userId)
      .status(AuthorityDataStatStatus.COMPLETED_SUCCESS)
      .build();
  }

  public static ResultList<UsersClient.User> usersList(List<UUID> userIds) {
    return ResultList.of(2, List.of(
      new UsersClient.User(
        userIds.get(0).toString(),
        "john_doe",
        true,
        new UsersClient.User.Personal("John", "Doe")
      ),
      new UsersClient.User(
        userIds.get(1).toString(),
        "quick_fox",
        true,
        new UsersClient.User.Personal("Quick", "Brown")
      )
    ));
  }

  public static AuthorityStatsDto getStatDataDto(AuthorityDataStat dataStat, UsersClient.User user) {
    AuthorityStatsDto dto = new AuthorityStatsDto();
    dto.setId(dataStat.getId());
    dto.setAuthorityId(dataStat.getAuthorityData().getId());
    dto.setAction(LinkAction.fromValue(dataStat.getAction().name()));
    dto.setHeadingNew(dataStat.getHeadingNew());
    dto.setHeadingOld(dataStat.getHeadingOld());
    dto.setHeadingTypeNew(dataStat.getHeadingTypeNew());
    dto.setHeadingTypeOld(dataStat.getHeadingTypeOld());
    dto.setLbUpdated(dataStat.getLbUpdated());
    dto.setLbFailed(dataStat.getLbFailed());
    dto.setLbTotal(dataStat.getLbTotal());
    dto.setNaturalIdNew(dataStat.getAuthorityNaturalIdNew());
    dto.setNaturalIdOld(dataStat.getAuthorityNaturalIdOld());
    AuthorityControlMetadata metadata = new AuthorityControlMetadata();
    metadata.setStartedByUserId(dataStat.getStartedByUserId());
    metadata.setStartedByUserFirstName(user.personal().firstName());
    metadata.setStartedByUserLastName(user.personal().lastName());
    metadata.setStartedAt(fromTimestamp(dataStat.getStartedAt()));
    metadata.setCompletedAt(fromTimestamp(dataStat.getCompletedAt()));
    dto.setMetadata(metadata);
    dto.setSourceFileNew(dataStat.getAuthoritySourceFileNew().toString());
    dto.setSourceFileOld(dataStat.getAuthoritySourceFileOld().toString());
    return dto;
  }

  public static StrippedParsedRecordCollection getAuthorityRecordsCollection(List<InstanceAuthorityLink> validLinks,
                                                                             List<InstanceAuthorityLink> invalidLinks) {
    var authorityRecords = ListUtils.union(
      getAuthorityRecords(validLinks, true),
      getAuthorityRecords(invalidLinks, false)
    );

    return new StrippedParsedRecordCollection(authorityRecords, authorityRecords.size());
  }

  public static StrippedParsedRecordCollection getAuthorityRecordsCollection(List<InstanceAuthorityLink> links) {
    var authorityRecords = getAuthorityRecords(links, true);
    return new StrippedParsedRecordCollection(authorityRecords, authorityRecords.size());
  }

  public static List<StrippedParsedRecord> getAuthorityRecords(List<InstanceAuthorityLink> links, Boolean valid) {
    return links.stream()
      .map(link -> {
        var existenceValidations = link.getLinkingRule().getSubfieldsExistenceValidations();
        var subfields = new LinkedList<Map<String, String>>();
        subfields.add(Map.of("a", "test"));
        if (existenceValidations != null && valid.equals(existenceValidations.get("t"))) {
          subfields.add(Map.of("t", "test"));
        }

        var field = Map.of(link.getLinkingRule().getAuthorityField(), new FieldContent().subfields(subfields));
        var recordContent = new ParsedRecordContent(singletonList(field), "leader");
        var parsedRecord = new StrippedParsedRecordParsedRecord(recordContent);

        return new StrippedParsedRecord(UUID.randomUUID(), RecordType.MARC_AUTHORITY, parsedRecord)
          .externalIdsHolder(new ExternalIdsHolder().authorityId(link.getAuthorityData().getId()));
      })
      .toList();
  }

  public record Link(UUID authorityId, String tag, String naturalId,
                     char[] subfields, int linkingRuleId,
                     InstanceAuthorityLinkStatus status, String errorCause) {

    public static final UUID[] AUTH_IDS = new UUID[] {UUID.fromString("845642cf-d4eb-4c2e-a067-db580c9a1abd"),
      UUID.fromString("1b8867a1-2f1d-4f6a-8023-5abaf980c24c"),
      UUID.fromString("1c8f571c-eff8-43fa-90a5-2dca70a35f2d"),
      UUID.fromString("91c3d682-7a6b-4c6f-802b-b2793e591fa4")};
    public static final String[] TAGS = new String[] {"100", "240", "700", "710"};
    public static final String[] AUTHORITY_TAGS = new String[] {"100", "110"};
    public static final Map<String, Map<String, Boolean>> SUBFIELD_VALIDATIONS_BY_TAG = Map.of(
      TAGS[0], Map.of("t", false),
      TAGS[1], Map.of("t", true),
      TAGS[2], emptyMap(),
      TAGS[3], emptyMap()
    );
    public static final Map<String, Integer> TAGS_TO_RULE_IDS = Map.of(
      TAGS[0], 1,
      TAGS[1], 5,
      TAGS[2], 15,
      TAGS[3], 16
    );
    public static final Map<String, String> TAGS_TO_AUTHORITY_TAGS = Map.of(
      TAGS[0], AUTHORITY_TAGS[0],
      TAGS[1], AUTHORITY_TAGS[0],
      TAGS[2], AUTHORITY_TAGS[0],
      TAGS[3], AUTHORITY_TAGS[1]
    );
    public static final Map<Integer, String> RULE_IDS_TO_TAGS = TAGS_TO_RULE_IDS.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    public static final Map<String, String> TAGS_TO_SUBFIELDS = Map.of(
      TAGS[0], "abcdjq",
      TAGS[1], "fghklmnoprsa",
      TAGS[2], "abcdjqfhklmnoprstg",
      TAGS[3], "abcfhklmoprstdgn"
    );

    public Link(UUID authorityId, String tag) {
      this(authorityId, tag, authorityId.toString(), TAGS_TO_SUBFIELDS.get(tag).toCharArray());
    }

    public Link(UUID authorityId, String tag, String naturalId, char[] subfields) {
      this(authorityId, tag, naturalId, subfields, RandomUtils.nextInt(1, 10), ACTUAL, null);
    }

    public static Link of(int authIdNum, int tagNum) {
      return new Link(AUTH_IDS[authIdNum], TAGS[tagNum]);
    }

    public static Link of(int authIdNum, int tagNum, String naturalId) {
      var tag = TAGS[tagNum];
      return new Link(AUTH_IDS[authIdNum], tag, naturalId, TAGS_TO_SUBFIELDS.get(tag).toCharArray());
    }

    public static Link of(InstanceAuthorityLinkStatus status, String errorCause) {
      return new Link(AUTH_IDS[0], TAGS[0], AUTH_IDS[0].toString(), TAGS_TO_SUBFIELDS.get(TAGS[0]).toCharArray(),
        1, status, errorCause);
    }

    public InstanceLinkDto toDto(UUID instanceId) {
      return new InstanceLinkDto()
        .instanceId(instanceId)
        .authorityId(authorityId)
        .authorityNaturalId(naturalId)
        .linkingRuleId(TAGS_TO_RULE_IDS.get(tag))
        .status(status.toString())
        .errorCause(errorCause);
    }

    public InstanceAuthorityLink toEntity(UUID instanceId) {
      return InstanceAuthorityLink.builder()
        .instanceId(instanceId)
        .authorityData(AuthorityData.builder()
          .id(authorityId)
          .naturalId(naturalId)
          .build())
        .linkingRule(InstanceAuthorityLinkingRule.builder()
          .id(TAGS_TO_RULE_IDS.get(tag))
          .bibField(tag)
          .authoritySubfields(new char[] {'a'})
          .authorityField(TAGS_TO_AUTHORITY_TAGS.get(tag))
          .subfieldsExistenceValidations(SUBFIELD_VALIDATIONS_BY_TAG.get(tag))
          .build())
        .status(status)
        .errorCause(errorCause)
        .build();
    }
  }
}
