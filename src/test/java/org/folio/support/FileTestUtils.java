package org.folio.support;

import static org.springframework.util.ResourceUtils.getFile;

import java.nio.file.Files;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FileTestUtils {

  @SneakyThrows
  public static String readFile(String filePath) {
    return new String(Files.readAllBytes(getFile(filePath).toPath()));
  }
}
