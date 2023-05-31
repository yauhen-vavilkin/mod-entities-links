package org.folio.entlinks.integration.internal;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyInt;
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
    var e1 = new AuthoritySourceFile(UUID.randomUUID(), "url1", "source-file-name-1", codes("e1"));
    var e2 = new AuthoritySourceFile(UUID.randomUUID(), "url2", "source-file-name-2", codes("e2"));
    var sourceFiles = List.of(e1, e2);

    when(client.fetchAuthoritySourceFiles(anyInt())).thenReturn(new AuthoritySourceFiles(sourceFiles));

    var actual = service.fetchAuthoritySources();

    assertThat(actual)
      .hasSize(sourceFiles.size())
      .contains(entry(e1.id(), e1), entry(e2.id(), e2));
  }

  @Test
  void fetchAuthoritySourceUrls_positive_ignoreNullValues() {
    var e1 = new AuthoritySourceFile(UUID.randomUUID(), "url1", "source-file-name-1", codes("e1"));
    var e2 = new AuthoritySourceFile(null, "url2", "source-file-name-2", codes("e2"));
    var e3 = new AuthoritySourceFile(UUID.randomUUID(), "url3", "source-file-name-3", codes("e3"));
    var e4 = new AuthoritySourceFile(UUID.randomUUID(), null, null, codes("e4"));
    var validSourceFiles = List.of(e1, e3);
    var sourceFiles = List.of(e1, e2, e3, e4);

    when(client.fetchAuthoritySourceFiles(anyInt())).thenReturn(new AuthoritySourceFiles(sourceFiles));

    var actual = service.fetchAuthoritySources();

    assertThat(actual)
      .hasSize(validSourceFiles.size())
      .contains(entry(e1.id(), e1), entry(e3.id(), e3));
  }

  @Test
  void fetchAuthoritySourceUrls_negative_emptyResponse() {
    when(client.fetchAuthoritySourceFiles(anyInt())).thenReturn(new AuthoritySourceFiles(emptyList()));

    assertThatThrownBy(() -> service.fetchAuthoritySources())
      .isInstanceOf(FolioIntegrationException.class)
      .hasMessage("Authority source files are empty");
  }

  @Test
  void fetchAuthoritySourceUrls_negative_clientException() {
    var cause = new IllegalArgumentException("test message");
    when(client.fetchAuthoritySourceFiles(anyInt())).thenThrow(cause);

    assertThatThrownBy(() -> service.fetchAuthoritySources())
      .isInstanceOf(FolioIntegrationException.class)
      .hasCauseExactlyInstanceOf(cause.getClass())
      .hasMessage("Failed to fetch authority source files");

  }

  private List<String> codes(String... codes) {
    return List.of(codes);
  }
}
