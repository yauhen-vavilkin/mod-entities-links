package org.folio.entlinks.integration.dto.event;

import static org.folio.entlinks.integration.dto.event.DomainEventType.DELETE;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.folio.entlinks.domain.dto.AuthorityDto;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorityDomainEvent extends DomainEvent<AuthorityDto> {

  private AuthorityDeleteEventSubType deleteEventSubType;

  public AuthorityDomainEvent() {
    super();
  }

  @JsonCreator
  public AuthorityDomainEvent(@JsonProperty("id") UUID id,
                              @JsonProperty("old") AuthorityDto oldEntity,
                              @JsonProperty("new") AuthorityDto newEntity,
                              @JsonProperty("type") DomainEventType type,
                              @JsonProperty("deleteEventSubType") AuthorityDeleteEventSubType deleteEventSubType,
                              @JsonProperty("tenant") String tenant) {
    super(id, oldEntity, newEntity, type, tenant);
    this.deleteEventSubType = deleteEventSubType;
  }

  public AuthorityDomainEvent(UUID id, AuthorityDto oldEntity, AuthorityDto newEntity, DomainEventType type,
                              String tenant) {
    this(id, oldEntity, newEntity, type, null, tenant);
  }

  public AuthorityDomainEvent(UUID id) {
    this(id, null, null, null, null, null);
  }

  public static AuthorityDomainEvent softDeleteEvent(UUID id, AuthorityDto oldEntity, String tenant) {
    return new AuthorityDomainEvent(id, oldEntity, null, DELETE, AuthorityDeleteEventSubType.SOFT_DELETE, tenant);
  }

  public static AuthorityDomainEvent hardDeleteEvent(UUID id, AuthorityDto oldEntity, String tenant) {
    return new AuthorityDomainEvent(id, oldEntity, null, DELETE, AuthorityDeleteEventSubType.HARD_DELETE, tenant);
  }
}
