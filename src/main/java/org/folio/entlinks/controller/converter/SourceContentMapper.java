package org.folio.entlinks.controller.converter;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.folio.entlinks.domain.dto.FieldContent;
import org.folio.entlinks.domain.dto.ParsedRecordContent;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.entlinks.domain.dto.StrippedParsedRecord;
import org.folio.entlinks.domain.dto.StrippedParsedRecordCollection;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.integration.dto.AuthorityParsedContent;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SourceContentMapper {

  default ParsedRecordContentCollection convertToParsedContentCollection(List<SourceParsedContent> listOfContent) {
    return new ParsedRecordContentCollection()
      .records(listOfContent.stream()
        .map(this::convertToParsedContent)
        .toList());
  }

  default ParsedRecordContent convertToParsedContent(SourceParsedContent content) {
    var fields = convertFieldsToListOfMaps(content.getFields());

    return new ParsedRecordContent(fields, content.getLeader());
  }

  default List<SourceParsedContent> convertToParsedContent(ParsedRecordContentCollection contentCollection) {
    return contentCollection.getRecords().stream()
      .map(this::convertToParsedContent)
      .toList();
  }

  default SourceParsedContent convertToParsedContent(ParsedRecordContent content) {
    var fields = convertFieldsToOneMap(content.getFields());

    return new SourceParsedContent(UUID.randomUUID(), content.getLeader(), fields);
  }

  default List<AuthorityParsedContent> convertToAuthorityParsedContent(StrippedParsedRecordCollection recordCollection,
                                                                       List<AuthorityData> authorityData) {
    return recordCollection.getRecords().stream()
      .map(r -> convertToAuthorityParsedContent(r, authorityData))
      .toList();
  }

  default AuthorityParsedContent convertToAuthorityParsedContent(StrippedParsedRecord parsedRecord,
                                                                 List<AuthorityData> authorityData) {
    var authorityId = parsedRecord.getExternalIdsHolder().getAuthorityId();
    var naturalId = extractNaturalId(authorityData, authorityId);
    var leader = parsedRecord.getParsedRecord().getContent().getLeader();
    var fields = convertFieldsToOneMap(parsedRecord.getParsedRecord().getContent().getFields());

    return new AuthorityParsedContent(authorityId, naturalId, leader, fields);
  }

  private List<Map<String, FieldContent>> convertFieldsToListOfMaps(Map<String, FieldParsedContent> fields) {
    return fields.entrySet().stream()
      .map(this::convertFieldParsedContent)
      .toList();
  }

  private Map<String, FieldParsedContent> convertFieldsToOneMap(List<Map<String, FieldContent>> fields) {
    return fields.stream()
      .flatMap(m -> m.entrySet().stream())
      .map(this::convertFieldContent)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private List<Map<String, String>> convertSubfieldsToListOfMaps(Map<String, String> subfields) {
    return subfields.entrySet().stream()
      .map(m -> Map.of(m.getKey(), m.getValue()))
      .toList();
  }

  private Map<String, String> convertSubfieldsToOneMap(List<Map<String, String>> subfields) {
    return subfields.stream()
      .flatMap(m -> m.entrySet().stream())
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map.Entry<String, FieldParsedContent> convertFieldContent(Map.Entry<String, FieldContent> fieldContent) {
    var ind1 = fieldContent.getValue().getInd1();
    var ind2 = fieldContent.getValue().getInd2();
    var linkDetails = fieldContent.getValue().getLinkDetails();
    var subfields = convertSubfieldsToOneMap(fieldContent.getValue().getSubfields());

    var fieldParsedContent = new FieldParsedContent(ind1, ind2, subfields, linkDetails);

    return new AbstractMap.SimpleEntry<>(fieldContent.getKey(), fieldParsedContent);
  }

  private Map<String, FieldContent> convertFieldParsedContent(Map.Entry<String, FieldParsedContent> fieldContent) {
    var ind1 = fieldContent.getValue().getInd1();
    var ind2 = fieldContent.getValue().getInd2();
    var linkDetails = fieldContent.getValue().getLinkDetails();
    var subfields = convertSubfieldsToListOfMaps(fieldContent.getValue().getSubfields());

    var fieldParsedContent = new FieldContent()
      .ind1(ind1).ind2(ind2)
      .linkDetails(linkDetails)
      .subfields(subfields);

    return Map.of(fieldContent.getKey(), fieldParsedContent);
  }

  private String extractNaturalId(List<AuthorityData> authorityData, UUID authorityId) {
    return authorityData.stream()
      .filter(data -> data.getId().equals(authorityId))
      .map(AuthorityData::getNaturalId)
      .findAny()
      .orElse(null);
  }
}
