package org.folio.entlinks.utils;

import static org.folio.entlinks.utils.FieldUtils.getSubfield0Value;
import static org.folio.entlinks.utils.FieldUtils.trimSubfield0Value;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.client.AuthoritySourceFileClient.AuthoritySourceFile;
import org.folio.spring.test.type.UnitTest;
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
    var expected = sourceFile.baseUrl() + '/' + NATURAL_ID;
    assertEquals(expected, actual);
  }

  @Test
  void shouldGetSubfield0ValueWithoutSourceFile() {
    var actual = getSubfield0Value(NATURAL_ID, null);
    assertEquals(NATURAL_ID, actual);
  }

  @Test
  void shouldFindSourceFileByNaturalIdAndReturnSubfield0() {
    var sourceFileN = getSourceFile("n");
    var sourceFileE = getSourceFile("e");
    var sourceFiles = Map.of(
      sourceFileN.id(), sourceFileN,
      sourceFileE.id(), sourceFileE
    );

    var actual = getSubfield0Value(sourceFiles, NATURAL_ID);
    var expected = sourceFileN.baseUrl() + '/' + NATURAL_ID;
    assertEquals(expected, actual);
  }

  private AuthoritySourceFile getSourceFile(String code) {
    return new AuthoritySourceFile(UUID.randomUUID(), BASE_URL + code, "testFile", List.of(code));
  }
}
