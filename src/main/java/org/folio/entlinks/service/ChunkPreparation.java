package org.folio.entlinks.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.Authority;
import org.folio.entlinks.client.MappingMetadataProvider;
import org.folio.processing.mapping.defaultmapper.processor.parameters.MappingParameters;
import org.folio.spring.FolioExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class ChunkPreparation {

  private final JdbcTemplate jdbcTemplate;
  private final FolioExecutionContext context;
  private final MappingMetadataProvider mappingMetadataProvider;
  private final SourceReader sourceReader;

  public void readAndMap(int chunkSize) {
    log.info("Start poc");
    var mappingMetadata = mappingMetadataProvider.getMappingMetadata();
    log.info("loaded mapping rules");
    var mappingRules = new JsonObject(mappingMetadata.mappingRules());
    var mappingParameters = new JsonObject(mappingMetadata.mappingParams()).mapTo(MappingParameters.class);
    var dbSchemaName = context
      .getFolioModuleMetadata()
      .getDBSchemaName(context.getTenantId());

    List<Chunk> chunks = getChunks(chunkSize, dbSchemaName);

    ObjectMapper objectMapper = getObjectMapper();
    for (Chunk chunk : chunks) {
      sourceReader.doMapping(mappingRules, mappingParameters, dbSchemaName, objectMapper, chunk);
    }

    log.info("End poc");
  }

  public static int getChunkAmount(int chunkSize, Integer totalRecords) {
    var chunkAmount = totalRecords / chunkSize;
    if ((chunkAmount * chunkSize) < totalRecords) {
      chunkAmount++;
    }
    return chunkAmount;
  }

  @NotNull
  private static ObjectMapper getObjectMapper() {
    var objectMapper = new ObjectMapper();
    objectMapper.configOverride(Authority.class).setInclude(
      JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null));
    return objectMapper;
  }

  private List<Chunk> getChunks(int chunkSize, String dbSchemaName) {
    var sql1 = String.format("SELECT count(*) FROM %s.marc_authority_view", dbSchemaName);
    log.info("do query: {}", sql1);
    var totalRecords =
      jdbcTemplate.queryForObject(sql1,
        Integer.class);
    log.info("Total records in db: {}", totalRecords);
    int chunkAmount = getChunkAmount(chunkSize, totalRecords);

    List<Chunk> chunks = new ArrayList<>();
    String startId = null;
    for (int i = 0; i < chunkAmount; i++) {
      var offset = chunkSize * (i + 1);
      if (offset > totalRecords) {
        chunks.add(new Chunk(i, startId, null));
      } else {
        var sql =
          String.format("SELECT marc_id FROM %s.marc_authority_view offset %s limit 1", dbSchemaName, offset);
        log.info("do query: {}", sql);
        var endId = jdbcTemplate.queryForObject(sql, String.class);
        chunks.add(new Chunk(i, startId, endId));
        startId = endId;
      }
    }
    return chunks;
  }

  record Chunk(int id, String startFrom, String endBy) { }
}
