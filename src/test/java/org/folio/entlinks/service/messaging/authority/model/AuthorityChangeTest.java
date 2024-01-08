package org.folio.entlinks.service.messaging.authority.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class AuthorityChangeTest {

  @Test
  void fromValue_positive() {
    var actual = AuthorityChangeField.fromValue("personalName");

    assertEquals(AuthorityChangeField.PERSONAL_NAME, actual);
  }

  @Test
  void fromValue_positive_ignoreCase() {
    var actual = AuthorityChangeField.fromValue("PersonalName");

    assertEquals(AuthorityChangeField.PERSONAL_NAME, actual);
  }

  @Test
  void fromValue_negative() {
    assertThrows(IllegalArgumentException.class, () -> AuthorityChangeField.fromValue("sourceFileId"));
  }
}
