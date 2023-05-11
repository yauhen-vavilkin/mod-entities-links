package org.folio.entlinks.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.folio.Authority;
import org.folio.processing.mapping.defaultmapper.MarcToAuthorityMapper;
import org.folio.processing.mapping.defaultmapper.processor.parameters.MappingParameters;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class SourceReader {

  private final JdbcTemplate jdbcTemplate;
  private final MarcToAuthorityMapper mapper;

  @Async
  public void doMapping(JsonObject mappingRules, MappingParameters mappingParameters, String dbSchemaName,
                        ObjectMapper objectMapper, ChunkPreparation.Chunk chunk) {
    log.info("startChunk_{}", chunk.id());
    var file = new File(String.format("authorities_%s.txt", chunk.id()));
    try {
      file.createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    var sb = new StringBuilder(String.format("SELECT * FROM %s.marc_authority_view WHERE ", dbSchemaName));
    if (chunk.startFrom() != null) {
      sb.append(String.format("marc_id >= ?", chunk.startFrom()));
    }
    if (chunk.endBy() != null) {
      if (chunk.startFrom() != null) {
        sb.append(" AND ");
      }
      sb.append(String.format("marc_id < ?", chunk.endBy()));
    }

    log.info("start::retrieve authority records from {} to {}", chunk.startFrom(), chunk.endBy());
    var resultStream = getResultStream(chunk, sb);
    log.info("finish::retrieve authority records from {} to {}", chunk.startFrom(), chunk.endBy());
    log.info("start::map authority records from {} to {}", chunk.startFrom(), chunk.endBy());
    var list = resultStream.stream()
      .map(sourceData -> {
        var marcSource = new JsonObject(sourceData.marc().toString());
        var authority = mapper.mapRecord(marcSource, mappingParameters, mappingRules);
        authority.setId(sourceData.authorityId());
        authority.setVersion(sourceData.version());
        authority.setSource(Authority.Source.MARC);
        return authority;
      })
      .map(authority -> {
        try {
          return objectMapper.writeValueAsString(authority);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      })
      .collect(Collectors.toList());
    log.info("finish::map authority records from {} to {}", chunk.startFrom(), chunk.endBy());
    writeToFile(list, file);
    list.clear();
    log.info("finishChunk_{}", chunk.id());
  }

  private List<SourceData> getResultStream(ChunkPreparation.Chunk chunk, StringBuilder sb) {
    return jdbcTemplate.query(
      con -> {
        var ps = con.prepareStatement(sb.toString());
        if (chunk.startFrom() != null) {
          ps.setObject(1, UUID.fromString(chunk.startFrom()));
          if (chunk.endBy() != null) {
            ps.setObject(2, UUID.fromString(chunk.endBy()));
          }
        } else {
          if (chunk.endBy() != null) {
            ps.setObject(1, UUID.fromString(chunk.endBy()));
          }
        }
        return ps;
      }, (rs, rowNum) -> {
        var authorityId = rs.getString("authority_id");
        var version = rs.getInt("v");
        var marc = rs.getObject("marc");
        return new SourceData(authorityId, marc, version);
      });
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

  record SourceData(String authorityId, Object marc, int version) {

  }
}
