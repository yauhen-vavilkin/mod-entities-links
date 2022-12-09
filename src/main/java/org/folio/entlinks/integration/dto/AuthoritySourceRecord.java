package org.folio.entlinks.integration.dto;

import java.util.UUID;
import org.marc4j.marc.Record;

public record AuthoritySourceRecord(UUID id, UUID snapshotId, Record content) {
}
