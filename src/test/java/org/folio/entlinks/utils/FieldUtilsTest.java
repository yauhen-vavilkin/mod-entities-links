package org.folio.entlinks.utils;

import static org.folio.entlinks.utils.FieldUtils.getSubfield0Value;
import static org.folio.entlinks.utils.FieldUtils.trimSubfield0Value;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class FieldUtilsTest {

  private static final String BASE_URL = "id.loc.gov/authorities/names/";
  private static final String NATURAL_ID = "n12345";

  @Test
  void shouldTrimSubfield0Value() {
    var given = BASE_URL + NATURAL_ID;
    var actual = trimSubfield0Value(given);

    assertEquals(NATURAL_ID, actual);
  }

  @Test
  void shouldTrimSubfield0ValueWithoutBaseUrl() {
    var actual = trimSubfield0Value(NATURAL_ID);
    assertEquals(NATURAL_ID, actual);
  }

  @Test
  void shouldGetSubfield0ValueWithSourceFile() {
    var sourceFile = getSourceFile("e");

    var actual = getSubfield0Value(NATURAL_ID, sourceFile);
    var expected = sourceFile.getBaseUrl() + '/' + NATURAL_ID;
    assertEquals(expected, actual);
  }

  @Test
  void shouldGetSubfield0ValueWithoutSourceFile() {
    var actual = getSubfield0Value(NATURAL_ID, null);
    assertEquals(NATURAL_ID, actual);
  }

  private AuthoritySourceFile getSourceFile(String code) {
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setId(UUID.randomUUID());
    sourceFile.setBaseUrl(BASE_URL + code);
    sourceFile.setName("testFile");
    var sourceFileCode = new AuthoritySourceFileCode();
    sourceFileCode.setCode(code);
    sourceFile.addCode(sourceFileCode);
    return sourceFile;
  }
}
