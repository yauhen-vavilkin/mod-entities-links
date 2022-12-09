package org.folio.support;

import static java.util.UUID.randomUUID;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.springframework.util.ResourceUtils.getFile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.InstanceLinkDto;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;

public class TestUtils {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

  @SneakyThrows
  public static String asJson(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

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

  @SneakyThrows
  public static String readFile(String filePath) {
    return new String(Files.readAllBytes(getFile(filePath).toPath()));
  }

  public record Link(UUID authorityId, String tag, String naturalId, char[] subfields) {

    public static final UUID[] AUTH_IDS = new UUID[] {randomUUID(), randomUUID(), randomUUID(), randomUUID()};
    public static final String[] TAGS = new String[] {"100", "101", "700", "710"};

    public Link(UUID authorityId, String tag) {
      this(authorityId, tag, authorityId.toString(), new char[]{'a', 'b'});
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
        .bibRecordTag(tag);
    }

    private List<String> toStringList(char[] subfields) {
      List<String> result = new ArrayList<>();
      for (char subfield : subfields) {
        result.add(Character.toString(subfield));
      }
      return result;
    }

    public InstanceAuthorityLink toEntity(UUID instanceId) {
      return InstanceAuthorityLink.builder()
        .instanceId(instanceId)
        .authorityId(authorityId)
        .authorityNaturalId(naturalId)
        .bibRecordSubfields(subfields)
        .bibRecordTag(tag)
        .build();
    }
  }
}
