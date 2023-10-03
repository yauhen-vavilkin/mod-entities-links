package org.folio.entlinks.service.reindex;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityIdentifier;
import org.folio.entlinks.domain.entity.AuthorityNote;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.HeadingRef;
import org.folio.entlinks.domain.entity.MetadataEntity;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class AuthorityReindexJobRunner implements ReindexJobRunner {

  private static final String COUNT_QUERY_TEMPLATE = "SELECT COUNT(*) FROM %s_mod_entities_links.authority";
  private static final String SELECT_QUERY_TEMPLATE =
      "SELECT * FROM %s_mod_entities_links.authority WHERE deleted = false";

  private final JdbcTemplate jdbcTemplate;
  private final FolioExecutionContext folioExecutionContext;
  private final ReindexService reindexService;
  private final AuthorityDomainEventPublisher eventPublisher;
  private final AuthorityMapper mapper;
  private final ObjectMapper objectMapper;

  @Async
  @Override
  @Transactional
  public void startReindex(ReindexJob reindexJob) {
    log.info("reindex::started");
    var reindexContext = new ReindexContext(reindexJob, folioExecutionContext);
    streamAuthorities(reindexContext);
    log.info("reindex::ended");
  }

  @Transactional(readOnly = true)
  public void streamAuthorities(ReindexContext context) {
    var totalRecords = jdbcTemplate.queryForObject(countQuery(context.getTenantId()), Integer.class);
    log.info("reindex::count={}", totalRecords);
    ReindexJobProgressTracker progressTracker = new ReindexJobProgressTracker(totalRecords == null ? 0 : totalRecords);

    TypeReference<HeadingRef[]> headingTypeRef = new TypeReference<>() { };
    TypeReference<AuthorityIdentifier[]> identifierTypeRef = new TypeReference<>() { };
    TypeReference<AuthorityNote[]> noteTypeRef = new TypeReference<>() { };
    jdbcTemplate.setFetchSize(50);
    var query = selectQuery(context.getTenantId());
    try (var authorityStream = jdbcTemplate.queryForStream(query,
        (rs, rowNum) -> toAuthority(rs, headingTypeRef, identifierTypeRef, noteTypeRef))) {
      authorityStream
        .forEach(authority -> {
          eventPublisher.publishReindexEvent(authority, context);
          progressTracker.incrementProcessedCount();
          reindexService.logJobProgress(progressTracker, context.getJobId());
        });
    } catch (Exception e) {
      log.warn(e);
      reindexService.logJobFailed(context.getJobId());
      return;
    }

    reindexService.logJobSuccess(context.getJobId());
  }

  private AuthorityDto toAuthority(ResultSet rs,
                                   TypeReference<HeadingRef[]> headingRefType,
                                   TypeReference<AuthorityIdentifier[]> identifierTypeRef,
                                   TypeReference<AuthorityNote[]> noteTypeRef) {
    var authority = new Authority();
    try {
      var id = rs.getString(Authority.ID_COLUMN);
      authority.setId(UUID.fromString(id));
      var naturalId = rs.getString(Authority.NATURAL_ID_COLUMN);
      authority.setNaturalId(naturalId);
      Optional.ofNullable(rs.getString(Authority.SOURCE_FILE_COLUMN))
          .ifPresent(sourceFileId -> {
            var sourceFile = new AuthoritySourceFile();
            sourceFile.setId(UUID.fromString(sourceFileId));
            authority.setAuthoritySourceFile(sourceFile);
          });
      var source = rs.getString(Authority.SOURCE_COLUMN);
      authority.setSource(source);
      var heading = rs.getString(Authority.HEADING_COLUMN);
      authority.setHeading(heading);
      var headingType = rs.getString(Authority.HEADING_TYPE_COLUMN);
      authority.setHeadingType(headingType);
      var version = rs.getInt(Authority.VERSION_COLUMN);
      authority.setVersion(version);
      var subjectHeadingCode = rs.getString(Authority.SUBJECT_HEADING_CODE_COLUMN);
      authority.setSubjectHeadingCode(subjectHeadingCode != null ? subjectHeadingCode.charAt(0) : null);

      var array = rs.getArray(Authority.SFT_HEADINGS_COLUMN);
      if (array != null) {
        var sftHeadings = objectMapper.readValue(array.toString(), headingRefType);
        authority.setSftHeadings(Arrays.asList(sftHeadings));
      }
      array = rs.getArray(Authority.SAFT_HEADINGS_COLUMN);
      if (array != null) {
        var saftHeadings = objectMapper.readValue(array.toString(), headingRefType);
        authority.setSaftHeadings(Arrays.asList(saftHeadings));
      }
      array = rs.getArray(Authority.IDENTIFIERS_COLUMN);
      if (array != null) {
        var identifiers = objectMapper.readValue(array.toString(), identifierTypeRef);
        authority.setIdentifiers(Arrays.asList(identifiers));
      }
      array = rs.getArray(Authority.NOTES_COLUMN);
      if (array != null) {
        var notes = objectMapper.readValue(array.toString(), noteTypeRef);
        authority.setNotes(Arrays.asList(notes));
      }

      var createdDate = rs.getTimestamp(MetadataEntity.CREATED_DATE_COLUMN);
      authority.setCreatedDate(createdDate);
      var createdBy = rs.getString(MetadataEntity.CREATED_BY_USER_COLUMN);
      authority.setCreatedByUserId(createdBy != null ? UUID.fromString(createdBy) : null);
      var updatedDate = rs.getTimestamp(MetadataEntity.UPDATED_DATE_COLUMN);
      authority.setUpdatedDate(updatedDate);
      var updatedBy = rs.getString(MetadataEntity.UPDATED_BY_USER_COLUMN);
      authority.setUpdatedByUserId(updatedBy != null ? UUID.fromString(updatedBy) : null);
    } catch (Exception e) {
      log.warn(e);
      throw new RuntimeException(e);
    }

    return mapper.toDto(authority);
  }

  private String countQuery(String tenant) {
    return String.format(COUNT_QUERY_TEMPLATE, tenant);
  }

  private String selectQuery(String tenant) {
    return String.format(SELECT_QUERY_TEMPLATE, tenant);
  }
}
