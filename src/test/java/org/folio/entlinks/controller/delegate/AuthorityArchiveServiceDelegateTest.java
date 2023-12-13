package org.folio.entlinks.controller.delegate;

import static org.folio.entlinks.client.SettingsClient.AuthoritiesExpirationSettingValue;
import static org.folio.entlinks.integration.SettingsService.AUTHORITIES_EXPIRE_SETTING_KEY;
import static org.folio.entlinks.integration.SettingsService.AUTHORITIES_EXPIRE_SETTING_SCOPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.entlinks.client.SettingsClient;
import org.folio.entlinks.config.properties.AuthorityArchiveProperties;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.repository.AuthorityArchiveRepository;
import org.folio.entlinks.integration.SettingsService;
import org.folio.entlinks.service.authority.AuthorityArchiveService;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityArchiveServiceDelegateTest {

  @Mock
  private AuthorityArchiveService authorityArchiveService;

  @Mock
  private SettingsService settingsService;

  @Mock
  private AuthorityArchiveRepository authorityArchiveRepository;

  @Mock
  private AuthorityArchiveProperties authorityArchiveProperties;

  @Mock
  private AuthorityDomainEventPublisher eventPublisher;

  @Mock
  private AuthorityMapper authorityMapper;

  @InjectMocks
  private AuthorityArchiveServiceDelegate delegate;

  @Test
  void shouldNotExpireAuthorityArchivesWhenOperationDisabledBySettings() {
    var setting = new SettingsClient.SettingEntry(UUID.randomUUID(), AUTHORITIES_EXPIRE_SETTING_SCOPE,
        AUTHORITIES_EXPIRE_SETTING_KEY, new AuthoritiesExpirationSettingValue(false, null));
    when(settingsService.getAuthorityExpireSetting()).thenReturn(Optional.of(setting));

    delegate.expire();

    verifyNoInteractions(authorityArchiveService);
    verifyNoInteractions(authorityArchiveRepository);
  }

  @Test
  void shouldExpireAuthorityArchivesWithDefaultRetentionPeriod() {
    var archive = new AuthorityArchive();
    var dto = new AuthorityDto();
    archive.setUpdatedDate(Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS)));
    when(authorityMapper.toDto(archive)).thenReturn(dto);
    when(settingsService.getAuthorityExpireSetting()).thenReturn(Optional.empty());
    when(authorityArchiveProperties.getRetentionPeriodInDays()).thenReturn(7);
    when(authorityArchiveRepository.streamByUpdatedTillDate(any(LocalDateTime.class))).thenReturn(Stream.of(archive));

    delegate.expire();

    verify(authorityArchiveService).delete(archive);
    verify(eventPublisher).publishHardDeleteEvent(dto);
  }

  @Test
  void shouldExpireAuthorityArchivesWithRetentionPeriodFromSettings() {
    var archive = new AuthorityArchive();
    var dto = new AuthorityDto();
    var setting = new SettingsClient.SettingEntry(UUID.randomUUID(), AUTHORITIES_EXPIRE_SETTING_SCOPE,
        AUTHORITIES_EXPIRE_SETTING_KEY, new AuthoritiesExpirationSettingValue(true, 1));
    archive.setUpdatedDate(Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS)));
    when(authorityMapper.toDto(archive)).thenReturn(dto);
    when(settingsService.getAuthorityExpireSetting()).thenReturn(Optional.of(setting));
    when(authorityArchiveRepository.streamByUpdatedTillDate(any(LocalDateTime.class))).thenReturn(Stream.of(archive));

    delegate.expire();

    verify(authorityArchiveService).delete(archive);
    verify(eventPublisher).publishHardDeleteEvent(dto);
  }
}
