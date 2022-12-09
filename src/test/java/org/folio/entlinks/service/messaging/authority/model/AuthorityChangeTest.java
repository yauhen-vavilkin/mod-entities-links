package org.folio.entlinks.service.messaging.authority.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class AuthorityChangeTest {

  @Test
  void fromValue_positive() {
    var actual = AuthorityChange.fromValue("personalName");

    assertEquals(AuthorityChange.PERSONAL_NAME, actual);
  }

  @Test
  void fromValue_positive_ignoreCase() {
    var actual = AuthorityChange.fromValue("PersonalName");

    assertEquals(AuthorityChange.PERSONAL_NAME, actual);
  }

  @Test
  void fromValue_negative() {
    assertThrows(IllegalArgumentException.class, () -> AuthorityChange.fromValue("sourceFileId"));
  }
}
