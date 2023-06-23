package org.folio.entlinks.service.links;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.repository.AuthorityDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorityDataService {

  private final AuthorityDataRepository repository;

  public Map<UUID, AuthorityData> saveAll(Collection<AuthorityData> authorityDataSet) {
    if (log.isDebugEnabled()) {
      log.debug("Saving authority data [authority data: {}]", authorityDataSet);
    } else {
      log.info("Saving authority data for {} authority", authorityDataSet.size());
    }
    return repository.saveAll(authorityDataSet).stream().collect(Collectors.toMap(AuthorityData::getId, ad -> ad));
  }

  @Transactional
  public void updateNaturalId(String naturalId, UUID authorityId) {
    log.info("Update authority data [authority id: {}, natural id: {}]", authorityId, naturalId);
    repository.updateNaturalIdById(naturalId, authorityId);
  }

  @Transactional
  public void markDeleted(Collection<UUID> ids) {
    log.info("Update authority data [authority ids: {}, deleted: true]", ids);
    repository.updateDeletedByIdIn(ids);
  }

  public List<AuthorityData> getByIdAndDeleted(Set<UUID> ids, Boolean deleted) {
    return repository.findByIdInAndDeleted(ids, deleted);
  }
}
