package org.folio.entlinks.service.dataloader;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.entity.AuthoritySourceFileSource;

public class SourceFileDeserializer extends StdDeserializer<AuthoritySourceFile> {

  SourceFileDeserializer() {
    this(null);
  }

  SourceFileDeserializer(Class<?> vc) {
    super(vc);
  }

  @SneakyThrows
  @Override
  public AuthoritySourceFile deserialize(JsonParser parser, DeserializationContext deserializer) {
    var sourceFile = new AuthoritySourceFile();
    ObjectCodec codec = parser.getCodec();
    JsonNode node = codec.readTree(parser);

    // get source file codes
    var iterator = node.get("codes").elements();
    while (iterator.hasNext()) {
      var codeNode = iterator.next();
      Optional.ofNullable(codeNode)
          .map(JsonNode::asText)
          .ifPresent(code -> {
            var sourceFileCode = new AuthoritySourceFileCode();
            sourceFileCode.setCode(code);
            sourceFile.addCode(sourceFileCode);
          });
    }
    var id = Optional.ofNullable(node.get("id")).map(JsonNode::asText).map(UUID::fromString).orElse(null);
    var type = Optional.ofNullable(node.get("type")).map(JsonNode::asText).orElse(null);
    var name = Optional.ofNullable(node.get("name")).map(JsonNode::asText).orElse(null);
    var url = Optional.ofNullable(node.get("baseUrl")).map(JsonNode::asText).orElse(null);
    final var source = Optional.ofNullable(node.get("source")).map(JsonNode::asText).orElse(null);

    sourceFile.setId(id);
    sourceFile.setName(name);
    sourceFile.setType(type);
    sourceFile.setBaseUrl(url);
    setSourceType(sourceFile, source);
    return sourceFile;
  }

  private static void setSourceType(AuthoritySourceFile sourceFile, String source) {
    if (source != null) {
      if (source.equals("folio")) {
        sourceFile.setSource(AuthoritySourceFileSource.FOLIO);
      } else {
        throw new IllegalArgumentException("Unknown source type: " + source);
      }
    }
  }
}
