package org.folio.entlinks.service.messaging.authority.model;

public record AuthorityChange(AuthorityChangeField changeField, Object valNew, Object valOld) {
}
