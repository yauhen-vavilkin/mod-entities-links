package org.folio.entlinks.integration.dto.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainEvent<T> implements BaseEvent {

  private UUID id;
  @JsonProperty("old")
  private T oldEntity;
  @JsonProperty("new")
  private T newEntity;
  private DomainEventType type;
  private String tenant;
  private String ts;

  @JsonCreator
  public DomainEvent(@JsonProperty("id") UUID id,
                     @JsonProperty("old") T oldEntity,
                     @JsonProperty("new") T newEntity,
                     @JsonProperty("type") DomainEventType type,
                     @JsonProperty("tenant") String tenant) {
    this.id = id;
    this.oldEntity = oldEntity;
    this.newEntity = newEntity;
    this.type = type;
    this.tenant = tenant;
  }

  public static <T> DomainEvent<T> createEvent(UUID id, T newEntity, String tenant) {
    return new DomainEvent<>(id, null, newEntity, DomainEventType.CREATE, tenant);
  }

  public static <T> DomainEvent<T> updateEvent(UUID id, T oldEntity, T newEntity, String tenant) {
    return new DomainEvent<>(id, oldEntity, newEntity, DomainEventType.UPDATE, tenant);
  }

  public static <T> DomainEvent<T> deleteEvent(UUID id, T oldEntity, String tenant) {
    return new DomainEvent<>(id, oldEntity, null, DomainEventType.DELETE, tenant);
  }

  public static <T> DomainEvent<T> reindexEvent(UUID id, T newEntity, String tenant) {
    return new DomainEvent<>(id, null, newEntity, DomainEventType.REINDEX, tenant);
  }

}
