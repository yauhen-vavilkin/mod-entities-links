package org.folio.entlinks.exception;

public class MarcAuthorityNotFoundException extends ResourceNotFoundException {

  private static final String RESOURCE_NAME = "Marc authority";

  public MarcAuthorityNotFoundException(Object authorityId) {
    super(RESOURCE_NAME, authorityId);
  }
}
