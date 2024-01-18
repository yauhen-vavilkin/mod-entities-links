package org.folio.entlinks.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class AuthoritySourceFileTest {

  @Test
  void testGetFullBaseUrl_WhenBaseUrlIsNull() {
    var authoritySourceFile = new AuthoritySourceFile();
    String actualResult = authoritySourceFile.getFullBaseUrl();
    assertNull(actualResult);
  }

  @Test
  void testGetFullBaseUrl_WhenBaseUrlIsNotNullButBaseUrlProtocolIsNull() {
    var authoritySourceFile = new AuthoritySourceFile();
    authoritySourceFile.setBaseUrl("www.example.com");
    String actualResult = authoritySourceFile.getFullBaseUrl();
    assertEquals("www.example.com", actualResult);
  }

  @Test
  void testGetFullBaseUrl_WhenBaseUrlAndBaseUrlProtocolAreNotNull() {
    var authoritySourceFile = new AuthoritySourceFile();
    authoritySourceFile.setBaseUrl("www.example.com");
    authoritySourceFile.setBaseUrlProtocol("https");
    String actualResult = authoritySourceFile.getFullBaseUrl();
    assertEquals("https://www.example.com", actualResult);
  }
}
