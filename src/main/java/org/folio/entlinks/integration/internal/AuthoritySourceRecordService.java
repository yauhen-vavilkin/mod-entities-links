package org.folio.entlinks.integration.internal;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.domain.dto.SourceParsedRecord;
import org.folio.entlinks.domain.dto.SourceRecord;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.integration.dto.AuthoritySourceRecord;
import org.marc4j.MarcJsonReader;
import org.marc4j.marc.Record;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthoritySourceRecordService {

  private final SourceStorageClient sourceStorageClient;
  private final ObjectMapper objectMapper;

  public AuthoritySourceRecord getAuthoritySourceRecordById(UUID authorityId) {
    log.info("Fetching source record [authorityId: {}]", authorityId);
    SourceRecord sourceRecord = fetchSourceRecord(authorityId);
    var content = extractMarcRecord(sourceRecord.getParsedRecord());
    return new AuthoritySourceRecord(authorityId, sourceRecord.getSnapshotId(), content);
  }

  private SourceRecord fetchSourceRecord(UUID authorityId) {
    try {
      return sourceStorageClient.getMarcAuthorityById(authorityId);
    } catch (Exception e) {
      throw new FolioIntegrationException("Failed to fetch source record [id: " + authorityId + "]", e);
    }
  }

  private Record extractMarcRecord(SourceParsedRecord parsedRecord) {
    try (var input = IOUtils.toInputStream(objectMapper.writeValueAsString(parsedRecord), UTF_8)) {
      return new MarcJsonReader(input).next();
    } catch (Exception e) {
      throw new FolioIntegrationException("Failed to get content of source record", e);
    }
  }
}
