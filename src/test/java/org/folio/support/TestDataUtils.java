package org.folio.support;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.entlinks.utils.DateUtils.fromTimestamp;
import static org.folio.support.base.TestConstants.TENANT_ID;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.folio.entlinks.domain.dto.AuthorityDataStatActionDto;
import org.folio.entlinks.domain.dto.AuthorityDataStatDto;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.BibStatsDto;
import org.folio.entlinks.domain.dto.BibStatsDtoCollection;
import org.folio.entlinks.domain.dto.InstanceLinkDto;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.dto.Metadata;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.folio.entlinks.domain.entity.AuthorityDataStatStatus;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
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
    return new InstanceLinkDtoCollection().links(links).totalRecords(links.size());
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
          .bibRecordTag("100")
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
        .bibRecordTag(link.getBibRecordTag())
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
        .bibRecordTag(link.getBibRecordTag())
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

  public static AuthorityDataStat authorityDataStat(UUID userId, AuthorityDataStatAction action) {
    return AuthorityDataStat.builder()
      .id(randomUUID())
      .action(action)
      .authorityData(AuthorityData.builder()
        .id(UUID.randomUUID())
        .naturalId("naturalIdNew")
        .build())
      .authorityNaturalIdOld("naturalIdOld")
      .authorityNaturalIdNew("naturalIdNew")
      .authoritySourceFileNew(UUID.randomUUID())
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

  public static AuthorityDataStatDto getStatDataDto(AuthorityDataStat dataStat, UsersClient.User user) {
    AuthorityDataStatDto dto = new AuthorityDataStatDto();
    dto.setId(dataStat.getId());
    dto.setAuthorityId(dataStat.getAuthorityData().getId());
    dto.setAction(AuthorityDataStatActionDto.fromValue(dataStat.getAction().name()));
    dto.setHeadingNew(dataStat.getHeadingNew());
    dto.setHeadingOld(dataStat.getHeadingOld());
    dto.setHeadingTypeNew(dataStat.getHeadingTypeNew());
    dto.setHeadingTypeOld(dataStat.getHeadingTypeOld());
    dto.setLbUpdated(dataStat.getLbUpdated());
    dto.setLbFailed(dataStat.getLbFailed());
    dto.setLbTotal(dataStat.getLbTotal());
    dto.setNaturalIdNew(dataStat.getAuthorityNaturalIdNew());
    dto.setNaturalIdOld(dataStat.getAuthorityNaturalIdOld());
    Metadata metadata = new Metadata();
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

  public record Link(UUID authorityId, String tag, String naturalId,
                     char[] subfields, int linkingRuleId) {

    public static final UUID[] AUTH_IDS = new UUID[] {randomUUID(), randomUUID(), randomUUID(), randomUUID()};
    public static final String[] TAGS = new String[] {"100", "101", "700", "710"};

    public Link(UUID authorityId, String tag) {
      this(authorityId, tag, authorityId.toString(), new char[] {'a', 'b'});
    }

    public Link(UUID authorityId, String tag, String naturalId, char[] subfields) {
      this(authorityId, tag, naturalId, subfields, RandomUtils.nextInt(1, 10));
    }

    public static Link of(int authIdNum, int tagNum) {
      return new Link(AUTH_IDS[authIdNum], TAGS[tagNum]);
    }

    public static Link of(int authIdNum, int tagNum, String naturalId, char[] subfields) {
      return new Link(AUTH_IDS[authIdNum], TAGS[tagNum], naturalId, subfields);
    }

    public InstanceLinkDto toDto(UUID instanceId) {
      return new InstanceLinkDto()
        .instanceId(instanceId)
        .authorityId(authorityId)
        .authorityNaturalId(naturalId)
        .bibRecordSubfields(toStringList(subfields))
        .bibRecordTag(tag)
        .linkingRuleId(1);
    }

    public InstanceAuthorityLink toEntity(UUID instanceId) {
      return InstanceAuthorityLink.builder()
        .instanceId(instanceId)
        .authorityData(AuthorityData.builder()
          .id(authorityId)
          .naturalId(naturalId)
          .build())
        .bibRecordSubfields(subfields)
        .bibRecordTag(tag)
        .linkingRuleId(1L)
        .build();
    }

    private List<String> toStringList(char[] subfields) {
      List<String> result = new ArrayList<>();
      for (char subfield : subfields) {
        result.add(Character.toString(subfield));
      }
      return result;
    }
  }
}
