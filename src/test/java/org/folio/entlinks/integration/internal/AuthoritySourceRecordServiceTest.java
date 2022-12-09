package org.folio.entlinks.integration.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.domain.dto.SourceRecord;
import org.folio.entlinks.domain.dto.SourceRecordParsedRecord;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.integration.dto.AuthoritySourceRecord;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marc4j.marc.VariableField;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthoritySourceRecordServiceTest {

  private static final String VALID_SOURCE_CONTENT = """
    {
      "content": {
        "fields": [
          {
            "001": "393893"
          },
          {
            "005": "20141107001016.0"
          },
          {
            "010": {
              "ind1": " ",
              "ind2": " ",
              "subfields": [
                {
                  "a": "  2001000234"
                }
              ]
            }
          }
        ],
        "leader": "06059cz  a2201201n  4500"
      }
    }
    """;

  private static final String INVALID_SOURCE_CONTENT = """
    {
      "content": {
        "fields": [
          {
            "qwert": {
              "ind1": " ",
              "ind2": " "
            }
          }
        ]
      }
    }
    """;

  private final ObjectMapper mapper = new ObjectMapper();

  private @Mock SourceStorageClient sourceStorageClient;

  private AuthoritySourceRecordService service;

  @BeforeEach
  void setUp() {
    service = new AuthoritySourceRecordService(sourceStorageClient, mapper);
  }

  @Test
  @SneakyThrows
  void getAuthoritySourceRecordById_positive() {
    var authorityId = UUID.randomUUID();
    var snapshotId = UUID.randomUUID();
    var sourceRecord = new SourceRecord()
      .recordId(authorityId)
      .snapshotId(snapshotId)
      .parsedRecord(mapper.readValue(VALID_SOURCE_CONTENT, SourceRecordParsedRecord.class));

    when(sourceStorageClient.getMarcAuthorityById(authorityId)).thenReturn(sourceRecord);

    var actual = service.getAuthoritySourceRecordById(authorityId);

    assertThat(actual)
      .extracting(AuthoritySourceRecord::id, AuthoritySourceRecord::snapshotId)
      .contains(authorityId, snapshotId);

    assertThat(actual.content().getDataFields())
      .hasSize(1)
      .extracting(VariableField::getTag)
      .contains("010");

    assertThat(actual.content().getControlFields())
      .hasSize(2)
      .extracting(VariableField::getTag)
      .contains("001", "005");
  }

  @Test
  @SneakyThrows
  void getAuthoritySourceRecordById_negative_clientException() {
    var authorityId = UUID.randomUUID();
    var cause = new IllegalArgumentException("test");

    when(sourceStorageClient.getMarcAuthorityById(authorityId)).thenThrow(cause);

    assertThatThrownBy(() -> service.getAuthoritySourceRecordById(authorityId))
      .isInstanceOf(FolioIntegrationException.class)
      .hasCauseExactlyInstanceOf(cause.getClass())
      .hasMessage("Failed to fetch source record [id: %s]", authorityId);
  }

  @Test
  @SneakyThrows
  void getAuthoritySourceRecordById_negative_recordParseException() {
    var authorityId = UUID.randomUUID();
    var snapshotId = UUID.randomUUID();
    var sourceRecord = new SourceRecord()
      .recordId(authorityId)
      .snapshotId(snapshotId)
      .parsedRecord(mapper.readValue(INVALID_SOURCE_CONTENT, SourceRecordParsedRecord.class));

    when(sourceStorageClient.getMarcAuthorityById(authorityId)).thenReturn(sourceRecord);

    assertThatThrownBy(() -> service.getAuthoritySourceRecordById(authorityId))
      .isInstanceOf(FolioIntegrationException.class)
      .hasMessage("Failed to get content of source record");
  }
}
