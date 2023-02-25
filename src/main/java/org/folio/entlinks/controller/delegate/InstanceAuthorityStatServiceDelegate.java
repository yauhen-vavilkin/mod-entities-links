package org.folio.entlinks.controller.delegate;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.converter.AuthorityDataStatMapper;
import org.folio.entlinks.domain.dto.AuthorityChangeStatDtoCollection;
import org.folio.entlinks.domain.dto.AuthorityDataStatActionDto;
import org.folio.entlinks.domain.dto.Metadata;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.entlinks.utils.DateUtils;
import org.folio.spring.tools.client.UsersClient;
import org.folio.spring.tools.model.ResultList;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InstanceAuthorityStatServiceDelegate {

  private final AuthorityDataStatService dataStatService;
  private final AuthorityDataStatMapper dataStatMapper;
  private final UsersClient usersClient;

  public AuthorityChangeStatDtoCollection fetchAuthorityLinksStats(OffsetDateTime fromDate, OffsetDateTime toDate,
                                                                   AuthorityDataStatActionDto action, Integer limit) {
    List<AuthorityDataStat> dataStatList = dataStatService.fetchDataStats(fromDate, toDate, action, limit + 1);

    Optional<AuthorityDataStat> last = Optional.empty();
    if (dataStatList.size() > limit) {
      last = Optional.of(dataStatList.get(limit));
      last.ifPresent(dataStatList::remove);
    }

    String query = getUsersQueryString(dataStatList);
    ResultList<UsersClient.User> userResultList =
      query.isEmpty() ? ResultList.of(0, Collections.emptyList()) : usersClient.query(query);
    var stats = dataStatList.stream()
      .map(source -> {
        Metadata metadata = getMetadata(userResultList, source);
        var authorityDataStatDto = dataStatMapper.convertToDto(source);
        authorityDataStatDto.setMetadata(metadata);
        return authorityDataStatDto;
      })
      .toList();

    return new AuthorityChangeStatDtoCollection()
      .stats(stats)
      .next(last.map(authorityDataStat -> DateUtils.fromTimestamp(authorityDataStat.getStartedAt()))
        .orElse(null));
  }

  private Metadata getMetadata(ResultList<UsersClient.User> userResultList, AuthorityDataStat source) {
    if (userResultList == null) {
      return null;
    }

    var user = userResultList.getResult()
      .stream()
      .filter(u -> UUID.fromString(u.id()).equals(source.getStartedByUserId()))
      .findFirst().orElse(null);
    if (user == null) {
      return null;
    }

    Metadata metadata = new Metadata();
    metadata.setStartedByUserFirstName(user.personal().firstName());
    metadata.setStartedByUserLastName(user.personal().lastName());
    metadata.setStartedByUserId(UUID.fromString(user.id()));
    metadata.setStartedAt(DateUtils.fromTimestamp(source.getStartedAt()));
    metadata.setCompletedAt(DateUtils.fromTimestamp(source.getCompletedAt()));
    return metadata;
  }

  private String getUsersQueryString(List<AuthorityDataStat> dataStatList) {
    var userIds = dataStatList.stream()
      .map(AuthorityDataStat::getStartedByUserId)
      .filter(Objects::nonNull)
      .map(UUID::toString)
      .distinct()
      .collect(Collectors.joining(" or "));
    return userIds.isEmpty() ? "" : "id=(" + userIds + ")";
  }
}
