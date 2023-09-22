package org.folio.entlinks.service.reindex.event;

import static org.folio.entlinks.service.reindex.event.DomainEventType.CREATE;
import static org.folio.entlinks.service.reindex.event.DomainEventType.DELETE;
import static org.folio.entlinks.service.reindex.event.DomainEventType.REINDEX;
import static org.folio.entlinks.service.reindex.event.DomainEventType.UPDATE;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Data;
import org.folio.entlinks.integration.dto.BaseEvent;

@Data
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
    return new DomainEvent<>(id, null, newEntity, CREATE, tenant);
  }

  public static <T> DomainEvent<T> updateEvent(UUID id, T oldEntity, T newEntity, String tenant) {
    return new DomainEvent<>(id, oldEntity, newEntity, UPDATE, tenant);
  }

  public static <T> DomainEvent<T> deleteEvent(UUID id, T oldEntity, String tenant) {
    return new DomainEvent<>(id, oldEntity, null, DELETE, tenant);
  }

  public static <T> DomainEvent<T> reindexEvent(UUID id, T newEntity, String tenant) {
    return new DomainEvent<>(id, null, newEntity, REINDEX, tenant);
  }

}
