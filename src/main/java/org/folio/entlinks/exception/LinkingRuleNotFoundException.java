package org.folio.entlinks.exception;

public class LinkingRuleNotFoundException extends ResourceNotFoundException {

  private static final String RESOURCE_NAME = "Linking rule";

  public LinkingRuleNotFoundException(Object id) {
    super(RESOURCE_NAME, id);
  }
}
