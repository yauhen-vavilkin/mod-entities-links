package org.folio.entlinks.service.reindex.event;

import static org.folio.entlinks.service.reindex.event.DomainEventType.CREATE;
import static org.folio.entlinks.service.reindex.event.DomainEventType.DELETE;
import static org.folio.entlinks.service.reindex.event.DomainEventType.REINDEX;
import static org.folio.entlinks.service.reindex.event.DomainEventType.UPDATE;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.folio.entlinks.integration.dto.BaseEvent;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainEvent<T> implements BaseEvent {

  @JsonProperty("old")
  private T oldEntity;
  @JsonProperty("new")
  private T newEntity;
  private DomainEventType type;
  private String tenant;
  private String ts;

  @JsonCreator
  public DomainEvent(@JsonProperty("old") T oldEntity,
                     @JsonProperty("new") T newEntity,
                     @JsonProperty("type") DomainEventType type,
                     @JsonProperty("tenant") String tenant) {
    this.oldEntity = oldEntity;
    this.newEntity = newEntity;
    this.type = type;
    this.tenant = tenant;
  }

  public static <T> DomainEvent<T> createEvent(T newEntity, String tenant) {
    return new DomainEvent<>(null, newEntity, CREATE, tenant);
  }

  public static <T> DomainEvent<T> updateEvent(T oldEntity, T newEntity, String tenant) {
    return new DomainEvent<>(oldEntity, newEntity, UPDATE, tenant);
  }

  public static <T> DomainEvent<T> deleteEvent(T oldEntity, String tenant) {
    return new DomainEvent<>(oldEntity, null, DELETE, tenant);
  }

  public static <T> DomainEvent<T> reindexEvent(String tenant, T newEntity) {
    return new DomainEvent<>(null, newEntity, REINDEX, tenant);
  }

}
