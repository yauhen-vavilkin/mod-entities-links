package org.folio.entlinks.integration.internal;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.entlinks.client.AuthoritySourceFileClient;
import org.folio.entlinks.client.AuthoritySourceFileClient.AuthoritySourceFile;
import org.folio.entlinks.client.AuthoritySourceFileClient.AuthoritySourceFiles;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthoritySourceFilesServiceTest {

  private @Mock AuthoritySourceFileClient client;
  private @InjectMocks AuthoritySourceFilesService service;

  @Test
  void fetchAuthoritySourceUrls_positive() {
    var e1 = new AuthoritySourceFile(UUID.randomUUID(), "url1");
    var e2 = new AuthoritySourceFile(UUID.randomUUID(), "url2");
    var sourceFiles = List.of(e1, e2);

    when(client.fetchAuthoritySourceFiles()).thenReturn(new AuthoritySourceFiles(sourceFiles));

    var actual = service.fetchAuthoritySourceUrls();

    assertThat(actual)
      .hasSize(sourceFiles.size())
      .contains(entry(e1.id(), e1.baseUrl()), entry(e2.id(), e2.baseUrl()));
  }

  @Test
  void fetchAuthoritySourceUrls_negative_emptyResponse() {
    when(client.fetchAuthoritySourceFiles()).thenReturn(new AuthoritySourceFiles(emptyList()));

    assertThatThrownBy(() -> service.fetchAuthoritySourceUrls())
      .isInstanceOf(FolioIntegrationException.class)
      .hasMessage("Authority source files are empty.");
  }

  @Test
  void fetchAuthoritySourceUrls_negative_clientException() {
    var cause = new IllegalArgumentException("test message");
    when(client.fetchAuthoritySourceFiles()).thenThrow(cause);

    assertThatThrownBy(() -> service.fetchAuthoritySourceUrls())
      .isInstanceOf(FolioIntegrationException.class)
      .hasCauseExactlyInstanceOf(cause.getClass())
      .hasMessage("Failed to fetch authority source files");

  }
}
