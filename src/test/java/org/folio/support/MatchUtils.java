package org.folio.support;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.folio.entlinks.domain.dto.BibStatsDto;
import org.folio.entlinks.domain.dto.BibStatsDtoCollection;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.springframework.test.web.servlet.ResultMatcher;

@UtilityClass
public class MatchUtils {

  public static ResultMatcher errorParameterMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].parameters.[0].key", errorMessageMatcher);
  }

  public static ResultMatcher errorTypeMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].type", errorMessageMatcher);
  }

  public static ResultMatcher errorCodeMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].code", errorMessageMatcher);
  }

  public static ResultMatcher errorMessageMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].message", errorMessageMatcher);
  }

  public static ResultMatcher errorTotalMatch(int errorTotal) {
    return jsonPath("$.total_records", is(errorTotal));
  }

  public static ResultMatcher statsMatch(Matcher<Collection<? extends BibStatsDto>> matcher) {
    return jsonPath("$.stats", matcher);
  }

  @SuppressWarnings("unchecked")
  public static ResultMatcher statsMatch(BibStatsDtoCollection stats) {
    var statsMatchers = stats.getStats().stream()
      .map(MatchUtils.StatsMatcher::statsMatch)
      .toArray(Matcher[]::new);
    return jsonPath("$.stats", contains(statsMatchers));
  }

  private static final class StatsMatcher extends BaseMatcher<BibStatsDto> {

    private final BibStatsDto expectedStats;

    private StatsMatcher(BibStatsDto expectedStats) {
      this.expectedStats = expectedStats;
    }

    static MatchUtils.StatsMatcher statsMatch(BibStatsDto expectedStats) {
      return new MatchUtils.StatsMatcher(expectedStats);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean matches(Object actual) {
      if (actual instanceof LinkedHashMap actualStats) {
        return Objects.equals(expectedStats.getInstanceId().toString(), actualStats.get("instanceId"))
          && Objects.equals(expectedStats.getAuthorityNaturalId(), actualStats.get("authorityNaturalId"))
          && Objects.equals(expectedStats.getBibRecordTag(), actualStats.get("bibRecordTag"))
          && Objects.equals(expectedStats.getInstanceTitle(), actualStats.get("instanceTitle"))
          && expectedStats.getUpdatedAt().isAfter(OffsetDateTime.parse((String) actualStats.get("updatedAt")))
          && Objects.equals(expectedStats.getErrorCause(), actualStats.get("errorCause"));
      }

      return false;
    }

    @Override
    public void describeTo(Description description) {
      description.appendValue(expectedStats);
    }
  }
}
