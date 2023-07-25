package org.folio.entlinks.service.reindex;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorityReindexJobRunnerTest {

//  private @Mock JdbcTemplate jdbcTemplate;
//  private @Mock EventProducer<DomainEvent<?>> eventProducer;
//  private @Mock FolioExecutionContext folioExecutionContext;
//  private @Mock ReindexService reindexService;
//
//  private @InjectMocks AuthorityReindexJobRunner jobRunner;
//
//  @Test
//  void name() {
//    MockResultSet mockResultSet = new MockResultSet("1");
//    mockResultSet.addColumn("jsonb", new String[]{"1"});
//    when(jdbcTemplate.queryForObject(any(), any(Class.class))).thenReturn(1);
//    when(jdbcTemplate.queryForStream(anyString(), any())).thenAnswer(invocationOnMock -> {
//      var rowMapper = (RowMapper<Authority>) invocationOnMock.getArguments()[1];
//      return rowMapper.mapRow(mockResultSet, 1);
//    });
//
//    jobRunner.startReindex(new ReindexJob().withResourceName(ReindexJobResource.AUTHORITY).withId(UUID.randomUUID()));
//  }
}
