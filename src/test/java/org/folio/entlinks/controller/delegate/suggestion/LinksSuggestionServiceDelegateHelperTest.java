package org.folio.entlinks.controller.delegate.suggestion;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.folio.entlinks.domain.dto.AuthoritySearchParameter;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinksSuggestionServiceDelegateHelperTest {

  @Mock
  private LinksSuggestionsByAuthorityNaturalId linksSuggestionsByAuthorityNaturalId;
  @Mock
  private LinksSuggestionsByAuthorityId linksSuggestionsByAuthorityId;

  @InjectMocks
  private LinksSuggestionServiceDelegateHelper delegateHelper;

  @Test
  void getDelegate_positive_id() {
    var actual = delegateHelper.getDelegate(AuthoritySearchParameter.ID);
    assertThat(actual).isInstanceOf(LinksSuggestionsByAuthorityId.class);
  }

  @Test
  void getDelegate_positive_naturalId() {
    var actual = delegateHelper.getDelegate(AuthoritySearchParameter.NATURAL_ID);
    assertThat(actual).isInstanceOf(LinksSuggestionsByAuthorityNaturalId.class);
  }

  @ParameterizedTest
  @EnumSource(AuthoritySearchParameter.class)
  void getDelegate_positive_naturalId(AuthoritySearchParameter searchParam) {
    var actual = delegateHelper.getDelegate(searchParam);
    assertThat(actual).isNotNull();
  }

  @Test
  void getDelegate_negative_null() {
    assertThatThrownBy(() -> delegateHelper.getDelegate(null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("AuthoritySearchParameter must not be null.");
  }
}
