package org.folio.entlinks.service.reindex;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class AuthorityReindexJobRunner implements ReindexJobRunner {

  private static final String COUNT_QUERY = "SELECT COUNT(*) FROM %s_mod_entities_links.authority";
  private static final String SELECT_QUERY = "SELECT * FROM %s_mod_entities_links.authority";

  private final JdbcTemplate jdbcTemplate;
  private final FolioExecutionContext folioExecutionContext;
  private final ReindexService reindexService;
  private final AuthorityDomainEventPublisher eventPublisher;
  private final AuthorityMapper mapper;

  @Async
  @Override
  public void startReindex(ReindexJob reindexJob) {
    log.info("reindex::started");
    var reindexContext = new ReindexContext(reindexJob, folioExecutionContext);
    streamAuthorities(reindexContext);
    log.info("reindex::ended");
  }

  //@Transactional(readOnly = true)
  private void streamAuthorities(ReindexContext context) {
    var totalRecords = jdbcTemplate.queryForObject(String.format(COUNT_QUERY, context.getTenantId()), Integer.class);
    log.info("reindex::count={}", totalRecords);
    ReindexJobProgressTracker progressTracker = new ReindexJobProgressTracker(totalRecords == null ? 0 : totalRecords);

    try (var authorityStream = jdbcTemplate.queryForStream(String.format(SELECT_QUERY, context.getTenantId()),
        (rs, rowNum) -> toAuthority(rs))) {
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

    // should we check progressTracker.getProcessedCount() == progressTracker.getTotalRecords() and then log success ?
    reindexService.logJobSuccess(context.getJobId());
  }

  private AuthorityDto toAuthority(ResultSet rs) {
    var authority = new Authority();
    try {
      var id = rs.getString("id");
      authority.setId(UUID.fromString(id));
      var naturalId = rs.getString("natural_id");
      authority.setNaturalId(naturalId);
      var sourceFileId = rs.getString("source_file_id");
      var sourceFile = new AuthoritySourceFile();
      sourceFile.setId(UUID.fromString(sourceFileId));
      authority.setAuthoritySourceFile(sourceFile);
      var source = rs.getString("source");
      authority.setSource(source);
      var heading = rs.getString("heading");
      authority.setHeading(heading);
      var headingType = rs.getString("heading_type");
      authority.setHeadingType(headingType);
      var version = rs.getInt("_version");
      authority.setVersion(version);
      var subjectHeadingCode = rs.getString("subject_heading_code");
      authority.setSubjectHeadingCode(subjectHeadingCode != null ? subjectHeadingCode.charAt(0) : null);

      /*var array = rs.getArray("sft_headings");
      var sftHeadings = (HeadingRef[]) array.getArray();
      array = rs.getArray("saft_headings");
      var saftHeadings = (HeadingRef[]) array.getArray();
      array = rs.getArray("identifiers");
      var identifiers = (AuthorityIdentifier[]) array.getArray();
      array = rs.getArray("notes");
      var notes = (AuthorityNote[]) array.getArray();

      authority.setSftHeadings(Arrays.asList(sftHeadings));
      authority.setSaftHeadings(Arrays.asList(saftHeadings));
      authority.setIdentifiers(Arrays.asList(identifiers));
      authority.setNotes(Arrays.asList(notes));*/
    } catch (SQLException e) {
      log.warn(e);
      throw new RuntimeException(e);
    }

    return mapper.toDto(authority);
  }
}
