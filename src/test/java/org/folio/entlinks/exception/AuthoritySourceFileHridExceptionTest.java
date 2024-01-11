package org.folio.entlinks.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.folio.entlinks.exception.type.ErrorType;
import org.junit.jupiter.api.Test;

class AuthoritySourceFileHridExceptionTest {

  @Test
  void constructor_WithSourceFileId_ShouldSetErrorMessage() {
    // Arrange
    UUID sourceFileId = UUID.randomUUID();

    // Act
    AuthoritySourceFileHridException exception = new AuthoritySourceFileHridException(sourceFileId);

    // Assert
    String expectedErrorMessage = String.format(
      "Cannot get next HRID for source file [id: %s, cause: %s]",
      sourceFileId, "source file doesn't support HRID generation"
    );
    assertEquals(expectedErrorMessage, exception.getMessage());
    assertEquals(ErrorType.VALIDATION_ERROR, exception.getErrorType());
  }

  @Test
  void constructor_WithSourceFileIdAndCause_ShouldSetErrorMessageAndCause() {
    // Arrange
    UUID sourceFileId = UUID.randomUUID();
    Throwable cause = new RuntimeException("Test cause");

    // Act
    AuthoritySourceFileHridException exception = new AuthoritySourceFileHridException(sourceFileId, cause);

    // Assert
    String expectedErrorMessage = String.format(
      "Cannot get next HRID for source file [id: %s, cause: %s]",
      sourceFileId, "Test cause"
    );
    assertEquals(expectedErrorMessage, exception.getMessage());
    assertEquals(ErrorType.UNKNOWN_ERROR, exception.getErrorType());
    assertEquals(cause, exception.getCause());
  }
}
