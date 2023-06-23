package org.folio.entlinks.service.links;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.repository.AuthorityDataRepository;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityDataServiceTest {

  @Mock
  private AuthorityDataRepository repository;
  @InjectMocks
  private AuthorityDataService service;

  @Test
  void shouldReturnAuthorityDataByIdsAndDeleted() {
    var expectedIds = Set.of(UUID.randomUUID());
    var expectedDeleted = true;
    var expected = singletonList(new AuthorityData());
    when(repository.findByIdInAndDeleted(expectedIds, expectedDeleted))
      .thenReturn(expected);

    var actual = service.getByIdAndDeleted(expectedIds, expectedDeleted);

    Assertions.assertEquals(expected, actual);
  }
}
