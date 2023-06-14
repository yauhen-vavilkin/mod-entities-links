package org.folio.entlinks.utils;

import static java.util.Objects.nonNull;

import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.client.AuthoritySourceFileClient.AuthoritySourceFile;

@UtilityClass
public class FieldUtils {

  /**
   * Extract naturalId from subfield $0.
   *
   * @param zeroValue subfield $0 value as {@link String}
   * @return trimmed naturalId as {@link String}
   */
  public static String trimSubfield0Value(String zeroValue) {
    if (nonNull(zeroValue)) {
      var slashIndex = zeroValue.lastIndexOf('/');
      if (slashIndex != -1) {
        return zeroValue.substring(slashIndex + 1);
      }
    }
    return zeroValue;
  }

  /**
   * Returns subfield $0 value with baseUrl from sourceFile.
   *
   * @param naturalId  Authority natural id as {@link String}
   * @param sourceFile Authority source file as {@link AuthoritySourceFile}
   * @return subfield $0 value as {@link String}
   */
  public static String getSubfield0Value(String naturalId, AuthoritySourceFile sourceFile) {
    var subfield0Value = "";
    if (nonNull(naturalId) && nonNull(sourceFile)) {
      subfield0Value = StringUtils.appendIfMissing(sourceFile.baseUrl(), "/");
    }
    return subfield0Value + naturalId;
  }

  /**
   * Search for source file by naturalId and returns subfield $0 value.
   *
   * @param sourceFiles Map of authority source files,
   *                    Where Key - sourceFileId as {@link UUID}, Value - sourceFile {@link AuthoritySourceFile}
   * @param naturalId   Authority natural id
   * @return subfield $0 value as {@link String}
   */
  public static String getSubfield0Value(Map<UUID, AuthoritySourceFile> sourceFiles, String naturalId) {
    var sourceFile = sourceFiles.values().stream()
      .filter(file -> file.codes().stream().anyMatch(naturalId::startsWith))
      .findFirst().orElse(null);

    if (nonNull(sourceFile)) {
      return getSubfield0Value(naturalId, sourceFile);
    }
    return naturalId;
  }
}
