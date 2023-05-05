package org.folio.entlinks.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.folio.Authority;
import org.folio.entlinks.client.MappingMetadataProvider;
import org.folio.processing.mapping.defaultmapper.MarcToAuthorityMapper;
import org.folio.processing.mapping.defaultmapper.processor.parameters.MappingParameters;
import org.folio.spring.FolioExecutionContext;
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

  public void readAndMap() {
    log.info("Start poc");
    var mappingMetadata = mappingMetadataProvider.getMappingMetadata();
    log.info("loaded mapping rules");
    var mappingRules = new JsonObject(mappingMetadata.mappingRules());
    var mappingParameters = new JsonObject(mappingMetadata.mappingParams()).mapTo(MappingParameters.class);
    var dbSchemaName = context
      .getFolioModuleMetadata()
      .getDBSchemaName(context.getTenantId());

    var objectMapper = new ObjectMapper();
    objectMapper.configOverride(Authority.class).setInclude(
      JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null));
    var file = new File("authorities.txt");
    try {
      file.createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    jdbcTemplate.queryForStream(String.format("SELECT * FROM %s.marc_authority_view LIMIT 10000", dbSchemaName),
        (rs, rowNum) -> {
          log.info("retrieve and map authority record {}", rowNum);
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
      .forEach(s -> writeToFile(s, file));
    log.info("End poc");
  }

  private void writeToFile(String s, File file) {
    try (var writer = new FileWriterWithEncoding(file, StandardCharsets.UTF_8, true)) {
      writer.write(s + System.lineSeparator());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
