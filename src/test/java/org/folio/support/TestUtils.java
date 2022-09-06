package org.folio.support;

import static java.util.UUID.randomUUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.model.entity.InstanceLink;
import org.folio.qm.domain.dto.InstanceLinkDto;
import org.folio.qm.domain.dto.InstanceLinkDtoCollection;

public class TestUtils {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

  @SneakyThrows
  public static String asJson(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  public static List<InstanceLinkDto> linksDto(UUID instanceId, Link... links) {
    return Arrays.stream(links).map(link -> link.toDto(instanceId)).toList();
  }


  public static InstanceLinkDtoCollection linksDtoCollection(List<InstanceLinkDto> links) {
    return new InstanceLinkDtoCollection().links(links).totalRecords(links.size());
  }

  public static List<InstanceLink> links(UUID instanceId, Link... links) {
    return Arrays.stream(links).map(link -> link.toEntity(instanceId)).toList();
  }

  public record Link(UUID authorityId, String tag) {

    public static final UUID[] AUTH_IDS = new UUID[] {randomUUID(), randomUUID(), randomUUID(), randomUUID()};
    public static final String[] TAGS = new String[] {"100", "101", "700", "710"};

    public static Link of(int authIdNum, int tagNum) {
      return new Link(AUTH_IDS[authIdNum], TAGS[tagNum]);
    }

    public InstanceLinkDto toDto(UUID instanceId) {
      return new InstanceLinkDto()
        .instanceId(instanceId)
        .authorityId(authorityId)
        .authorityNaturalId(authorityId.toString())
        .bibRecordSubfields(List.of("a", "b"))
        .bibRecordTag(tag);
    }

    public InstanceLink toEntity(UUID instanceId) {
      return InstanceLink.builder()
        .instanceId(instanceId)
        .authorityId(authorityId)
        .authorityNaturalId(authorityId.toString())
        .bibRecordTag(tag)
        .bibRecordSubfields(List.of("a", "b"))
        .build();
    }
  }
}
