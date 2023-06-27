package org.folio.entlinks.controller.converter;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import java.util.LinkedHashMap;
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
      .map(parsedRecord -> convertToAuthorityParsedContent(parsedRecord, authorityData))
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

  private List<Map<String, FieldContent>> convertFieldsToListOfMaps(List<FieldParsedContent> fields) {
    return fields.stream()
      .map(this::convertParsedContent)
      .toList();
  }

  private List<FieldParsedContent> convertFieldsToOneMap(List<Map<String, FieldContent>> fields) {
    return fields.stream()
      .flatMap(map -> map.entrySet().stream())
      .map(this::convertFieldContent)
      .toList();
  }

  private List<Map<String, String>> convertSubfieldsToListOfMaps(Map<String, List<String>> subfields) {
    return subfields.entrySet().stream()
      .map(subfieldsByTag -> subfieldsByTag.getValue().stream()
        .map(subfieldValue -> Map.of(subfieldsByTag.getKey(), subfieldValue))
        .toList())
      .flatMap(List::stream)
      .toList();
  }

  private Map<String, List<String>> convertSubfieldsToOneMap(List<Map<String, String>> subfields) {
    return subfields.stream()
      .flatMap(map -> map.entrySet().stream())
      .collect(groupingBy(Map.Entry::getKey, LinkedHashMap::new, mapping(Map.Entry::getValue, Collectors.toList())));
  }

  private FieldParsedContent convertFieldContent(Map.Entry<String, FieldContent> fieldContent) {
    var ind1 = fieldContent.getValue().getInd1();
    var ind2 = fieldContent.getValue().getInd2();
    var linkDetails = fieldContent.getValue().getLinkDetails();
    var subfields = convertSubfieldsToOneMap(fieldContent.getValue().getSubfields());

    return new FieldParsedContent(fieldContent.getKey(), ind1, ind2, subfields, linkDetails);
  }

  private Map<String, FieldContent> convertParsedContent(FieldParsedContent field) {
    var ind1 = field.getInd1();
    var ind2 = field.getInd2();
    var linkDetails = field.getLinkDetails();
    var subfields = convertSubfieldsToListOfMaps(field.getSubfields());

    var fieldContent = new FieldContent().ind1(ind1).ind2(ind2)
      .linkDetails(linkDetails)
      .subfields(subfields);

    return Map.of(field.getTag(), fieldContent);
  }

  private String extractNaturalId(List<AuthorityData> authorityData, UUID authorityId) {
    return authorityData.stream()
      .filter(data -> data.getId().equals(authorityId))
      .map(AuthorityData::getNaturalId)
      .findAny()
      .orElse(null);
  }
}
