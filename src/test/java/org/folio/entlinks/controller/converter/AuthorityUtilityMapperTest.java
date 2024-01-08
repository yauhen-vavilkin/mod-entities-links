package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CORPORATE_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CORPORATE_NAME_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GENRE_TERM_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GEOGRAPHIC_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEETING_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEETING_NAME_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.PERSONAL_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.PERSONAL_NAME_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.TOPICAL_TERM_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.UNIFORM_TITLE_HEADING;
import static org.folio.support.base.TestConstants.TEST_STRING;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.HeadingRef;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class AuthorityUtilityMapperTest {

  private final AuthorityDto source = new AuthorityDto();
  private final Authority target = new Authority();

  @ParameterizedTest
  @MethodSource("headingTypeAndValueProvider")
  void testExtractAuthorityHeadingWithNonNullValues(String propertyType, String propertyValue) {
    switch (propertyType) {
      case PERSONAL_NAME_HEADING -> source.setPersonalName(propertyValue);
      case PERSONAL_NAME_TITLE_HEADING -> source.setPersonalNameTitle(propertyValue);
      case CORPORATE_NAME_HEADING -> source.setCorporateName(propertyValue);
      case CORPORATE_NAME_TITLE_HEADING -> source.setCorporateNameTitle(propertyValue);
      case MEETING_NAME_HEADING -> source.setMeetingName(propertyValue);
      case MEETING_NAME_TITLE_HEADING -> source.setMeetingNameTitle(propertyValue);
      case UNIFORM_TITLE_HEADING -> source.setUniformTitle(propertyValue);
      case TOPICAL_TERM_HEADING -> source.setTopicalTerm(propertyValue);
      case GEOGRAPHIC_NAME_HEADING -> source.setGeographicName(propertyValue);
      case GENRE_TERM_HEADING -> source.setGenreTerm(propertyValue);
      default -> fail("Invalid heading type - {} cannot be mapped", propertyType);
    }

    AuthorityUtilityMapper.extractAuthorityHeading(source, target);

    assertThat(propertyValue).isEqualTo(target.getHeading());
    assertThat(propertyType).isEqualTo(target.getHeadingType());
  }

  @ParameterizedTest
  @MethodSource("headingTypeAndValueProvider")
  void testExtractAuthoritySftHeadingsWithNonNullValues(String propertyType, String propertyValue) {
    switch (propertyType) {
      case PERSONAL_NAME_HEADING -> source.setSftPersonalName(Collections.singletonList(propertyValue));
      case PERSONAL_NAME_TITLE_HEADING -> source.setSftPersonalNameTitle(Collections.singletonList(propertyValue));
      case CORPORATE_NAME_HEADING -> source.setSftCorporateName(Collections.singletonList(propertyValue));
      case CORPORATE_NAME_TITLE_HEADING -> source.setSftCorporateNameTitle(Collections.singletonList(propertyValue));
      case MEETING_NAME_HEADING -> source.setSftMeetingName(Collections.singletonList(propertyValue));
      case MEETING_NAME_TITLE_HEADING -> source.setSftMeetingNameTitle(Collections.singletonList(propertyValue));
      case UNIFORM_TITLE_HEADING -> source.setSftUniformTitle(Collections.singletonList(propertyValue));
      case TOPICAL_TERM_HEADING -> source.setSftTopicalTerm(Collections.singletonList(propertyValue));
      case GEOGRAPHIC_NAME_HEADING -> source.setSftGeographicName(Collections.singletonList(propertyValue));
      case GENRE_TERM_HEADING -> source.setSftGenreTerm(Collections.singletonList(propertyValue));
      default -> fail("Invalid sft heading type - {} cannot be mapped", propertyType);
    }

    AuthorityUtilityMapper.extractAuthoritySftHeadings(source, target);

    List<HeadingRef> sftHeadings = target.getSftHeadings();
    assertThat(sftHeadings).hasSize(1);
    assertThat(propertyValue).isEqualTo(sftHeadings.get(0).getHeading());
    assertThat(propertyType).isEqualTo(sftHeadings.get(0).getHeadingType());
  }

  @ParameterizedTest
  @MethodSource("headingTypeAndValueProvider")
  void testExtractAuthoritySaftHeadingsWithNonNullValues(String propertyType, String propertyValue) {
    switch (propertyType) {
      case PERSONAL_NAME_HEADING -> source.setSaftPersonalName(Collections.singletonList(propertyValue));
      case PERSONAL_NAME_TITLE_HEADING -> source.setSaftPersonalNameTitle(Collections.singletonList(propertyValue));
      case CORPORATE_NAME_HEADING -> source.setSaftCorporateName(Collections.singletonList(propertyValue));
      case CORPORATE_NAME_TITLE_HEADING -> source.setSaftCorporateNameTitle(Collections.singletonList(propertyValue));
      case MEETING_NAME_HEADING -> source.setSaftMeetingName(Collections.singletonList(propertyValue));
      case MEETING_NAME_TITLE_HEADING -> source.setSaftMeetingNameTitle(Collections.singletonList(propertyValue));
      case UNIFORM_TITLE_HEADING -> source.setSaftUniformTitle(Collections.singletonList(propertyValue));
      case TOPICAL_TERM_HEADING -> source.setSaftTopicalTerm(Collections.singletonList(propertyValue));
      case GEOGRAPHIC_NAME_HEADING -> source.setSaftGeographicName(Collections.singletonList(propertyValue));
      case GENRE_TERM_HEADING -> source.setSaftGenreTerm(Collections.singletonList(propertyValue));
      default -> fail("Invalid saft heading type - {} cannot be mapped", propertyType);
    }

    AuthorityUtilityMapper.extractAuthoritySaftHeadings(source, target);

    List<HeadingRef> saftHeadings = target.getSaftHeadings();
    assertThat(saftHeadings).hasSize(1);
    assertThat(propertyValue).isEqualTo(saftHeadings.get(0).getHeading());
    assertThat(propertyType).isEqualTo(saftHeadings.get(0).getHeadingType());
  }

  @ParameterizedTest
  @MethodSource("headingTypeAndValueProvider")
  void testExtractAuthorityDtoSftHeadings(String headingType, String headingValue) {

    List<HeadingRef> sftHeadings = new ArrayList<>();
    sftHeadings.add(new HeadingRef(headingType, headingValue));
    target.setSftHeadings(sftHeadings);

    AuthorityUtilityMapper.extractAuthorityDtoSftHeadings(target, source);

    switch (headingType) {
      case PERSONAL_NAME_HEADING -> assertTrue(source.getSftPersonalName().contains(headingValue));
      case PERSONAL_NAME_TITLE_HEADING -> assertTrue(source.getSftPersonalNameTitle().contains(headingValue));
      case CORPORATE_NAME_HEADING -> assertTrue(source.getSftCorporateName().contains(headingValue));
      case CORPORATE_NAME_TITLE_HEADING -> assertTrue(source.getSftCorporateNameTitle().contains(headingValue));
      case MEETING_NAME_HEADING -> assertTrue(source.getSftMeetingName().contains(headingValue));
      case MEETING_NAME_TITLE_HEADING -> assertTrue(source.getSftMeetingNameTitle().contains(headingValue));
      case UNIFORM_TITLE_HEADING -> assertTrue(source.getSftUniformTitle().contains(headingValue));
      case TOPICAL_TERM_HEADING -> assertTrue(source.getSftTopicalTerm().contains(headingValue));
      case GEOGRAPHIC_NAME_HEADING -> assertTrue(source.getSftGeographicName().contains(headingValue));
      case GENRE_TERM_HEADING -> assertTrue(source.getSftGenreTerm().contains(headingValue));
      default -> fail("Invalid sft heading type - {} cannot be mapped", headingType);
    }
  }

  @ParameterizedTest
  @MethodSource("headingTypeAndValueProvider")
  void testExtractAuthorityDtoSaftHeadings(String headingType, String headingValue) {

    List<HeadingRef> saftHeadings = new ArrayList<>();
    saftHeadings.add(new HeadingRef(headingType, headingValue));
    target.setSaftHeadings(saftHeadings);

    AuthorityUtilityMapper.extractAuthorityDtoSaftHeadings(target, source);

    switch (headingType) {
      case PERSONAL_NAME_HEADING -> assertTrue(source.getSaftPersonalName().contains(headingValue));
      case PERSONAL_NAME_TITLE_HEADING -> assertTrue(source.getSaftPersonalNameTitle().contains(headingValue));
      case CORPORATE_NAME_HEADING -> assertTrue(source.getSaftCorporateName().contains(headingValue));
      case CORPORATE_NAME_TITLE_HEADING -> assertTrue(source.getSaftCorporateNameTitle().contains(headingValue));
      case MEETING_NAME_HEADING -> assertTrue(source.getSaftMeetingName().contains(headingValue));
      case MEETING_NAME_TITLE_HEADING -> assertTrue(source.getSaftMeetingNameTitle().contains(headingValue));
      case UNIFORM_TITLE_HEADING -> assertTrue(source.getSaftUniformTitle().contains(headingValue));
      case TOPICAL_TERM_HEADING -> assertTrue(source.getSaftTopicalTerm().contains(headingValue));
      case GEOGRAPHIC_NAME_HEADING -> assertTrue(source.getSaftGeographicName().contains(headingValue));
      case GENRE_TERM_HEADING -> assertTrue(source.getSaftGenreTerm().contains(headingValue));
      default -> fail("Invalid saft heading type - {} cannot be mapped", headingType);
    }
  }


  @ParameterizedTest
  @MethodSource("headingTypeAndValueProvider")
  void testExtractAuthorityDtoHeadingValue(String headingType, String headingValue) {
    target.setHeading(headingValue);
    target.setHeadingType(headingType);

    AuthorityUtilityMapper.extractAuthorityDtoHeadingValue(target, source);

    switch (headingType) {
      case PERSONAL_NAME_HEADING -> assertThat(source.getPersonalName()).isEqualTo(headingValue);
      case PERSONAL_NAME_TITLE_HEADING -> assertThat(source.getPersonalNameTitle()).isEqualTo(headingValue);
      case CORPORATE_NAME_HEADING -> assertThat(source.getCorporateName()).isEqualTo(headingValue);
      case CORPORATE_NAME_TITLE_HEADING -> assertThat(source.getCorporateNameTitle()).isEqualTo(headingValue);
      case MEETING_NAME_HEADING -> assertThat(source.getMeetingName()).isEqualTo(headingValue);
      case MEETING_NAME_TITLE_HEADING -> assertThat(source.getMeetingNameTitle()).isEqualTo(headingValue);
      case UNIFORM_TITLE_HEADING -> assertThat(source.getUniformTitle()).isEqualTo(headingValue);
      case TOPICAL_TERM_HEADING -> assertThat(source.getTopicalTerm()).isEqualTo(headingValue);
      case GEOGRAPHIC_NAME_HEADING -> assertThat(source.getGeographicName()).isEqualTo(headingValue);
      case GENRE_TERM_HEADING -> assertThat(source.getGenreTerm()).isEqualTo(headingValue);
      default -> fail("Invalid heading type - {} cannot be mapped", headingType);
    }
  }

  private static Stream<Arguments> headingTypeAndValueProvider() {
    return Stream.of(
        arguments(PERSONAL_NAME_HEADING, TEST_STRING),
        arguments(PERSONAL_NAME_TITLE_HEADING, TEST_STRING),
        arguments(CORPORATE_NAME_HEADING, TEST_STRING),
        arguments(CORPORATE_NAME_TITLE_HEADING, TEST_STRING),
        arguments(MEETING_NAME_HEADING, TEST_STRING),
        arguments(MEETING_NAME_TITLE_HEADING, TEST_STRING),
        arguments(UNIFORM_TITLE_HEADING, TEST_STRING),
        arguments(TOPICAL_TERM_HEADING, TEST_STRING),
        arguments(GEOGRAPHIC_NAME_HEADING, TEST_STRING),
        arguments(GENRE_TERM_HEADING, TEST_STRING)
    );
  }

}
