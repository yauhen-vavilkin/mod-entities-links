package org.folio.entlinks.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.folio.Authority;
import org.folio.entlinks.client.MappingMetadataProvider;
import org.folio.processing.mapping.defaultmapper.MarcToAuthorityMapper;
import org.folio.processing.mapping.defaultmapper.processor.parameters.MappingParameters;
import org.folio.spring.FolioExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class SourceReader {

  private final JdbcTemplate jdbcTemplate;
  private final MarcToAuthorityMapper mapper;
  private final FolioExecutionContext context;
  private final MappingMetadataProvider mappingMetadataProvider;

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
      var file = new File(String.format("authorities_%s.txt", chunk.id()));
      try {
        file.createNewFile();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      var sb = new StringBuilder(String.format("SELECT * FROM %s.marc_authority_view WHERE ", dbSchemaName));
      if (chunk.startFrom() != null) {
        sb.append(String.format("marc_id >= '%s'", chunk.startFrom()));
      }
      if (chunk.endBy() != null) {
        if (chunk.startFrom() != null) {
          sb.append(" AND ");
        }
        sb.append(String.format("marc_id < '%s'", chunk.endBy()));
      }
      log.info("retrieve and map authority records by query {}", sb.toString());
      var lines = jdbcTemplate.queryForStream(sb.toString(),
          (rs, rowNum) -> {
            var authorityId = rs.getString("authority_id");
            var version = rs.getInt("v");
            var marcSource = new JsonObject(rs.getObject("marc").toString());
            var authority = mapper.mapRecord(marcSource, mappingParameters, mappingRules);
            authority.setId(authorityId);
            authority.setVersion(version);
            return authority;
          })
        .map(authority -> {
          try {
            return objectMapper.writeValueAsString(authority);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        })
        .toList();
      writeToFile(lines, file);
    }

    log.info("End poc");
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
    var chunkAmount = totalRecords / chunkSize;
    if (chunkAmount * totalRecords < totalRecords) {
      chunkAmount++;
    }

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

  private void writeToFile(List<String> s, File file) {
    log.info("Write {} lines to file: {}", s.size(), file.getName());
    try (var writer = new FileWriterWithEncoding(file, StandardCharsets.UTF_8, true)) {
      for (String s1 : s) {
        writer.write(s1 + System.lineSeparator());
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } finally {
      log.info("File saved: {}", file.getName());
    }
  }

  record Chunk(int id, String startFrom, String endBy) { }
}
