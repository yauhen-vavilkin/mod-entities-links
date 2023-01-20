package org.folio.entlinks.service.links;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatStatus;
import org.folio.entlinks.domain.repository.AuthorityDataStatRepository;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorityDataStatService {

  private final AuthorityDataStatRepository statRepository;

  private final AuthorityDataService authorityDataService;

  public List<AuthorityDataStat> createInBatch(List<AuthorityDataStat> stats) {
    var authorityDataSet = stats.stream()
      .map(AuthorityDataStat::getAuthorityData)
      .collect(Collectors.toSet());
    var savedAuthorityData = authorityDataService.saveAll(authorityDataSet);

    for (AuthorityDataStat stat : stats) {
      stat.setId(UUID.randomUUID());
      stat.setStatus(AuthorityDataStatStatus.IN_PROGRESS);
      var authorityData = savedAuthorityData.get(stat.getAuthorityData().getId());
      stat.setAuthorityData(authorityData);
    }

    return statRepository.saveAll(stats);
  }
}
