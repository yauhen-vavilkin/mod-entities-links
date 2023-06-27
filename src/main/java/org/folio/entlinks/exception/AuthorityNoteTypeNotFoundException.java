package org.folio.entlinks.exception;

public class AuthorityNoteTypeNotFoundException extends ResourceNotFoundException {

  private static final String RESOURCE_NAME = "Authority Note Type";

  public AuthorityNoteTypeNotFoundException(Object id) {
    super(RESOURCE_NAME, id);
  }
}
