package org.folio.entlinks.service.consortium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ConsortiumTenantExecutorTest {

  @Mock
  private UserTenantsService userTenantsService;

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @Mock
  private SystemUserScopedExecutionService scopedExecutionService;

  @InjectMocks
  private ConsortiumTenantExecutor consortiumTenantExecutor;

  @Test
  void shouldNotExecuteOperationWhenTenantIsNotConsortiaMember() {
    var counter = new AtomicInteger();
    Supplier<Integer> operation = counter::incrementAndGet;
    var memberTenant = "memberId";

    when(folioExecutionContext.getTenantId()).thenReturn(memberTenant);
    when(userTenantsService.getCentralTenant(memberTenant)).thenReturn(Optional.empty());

    var result = consortiumTenantExecutor.executeAsCentralTenant(operation);

    assertNull(result);
    assertEquals(0, counter.get());
  }

  @Test
  void shouldExecuteMemberOperationInContextOfCentralTenant() {
    var counter = new AtomicInteger();
    Supplier<Integer> operation = counter::incrementAndGet;
    var memberTenant = "memberId";
    var centralTenant = "centralId";

    when(folioExecutionContext.getTenantId()).thenReturn(memberTenant);
    when(userTenantsService.getCentralTenant(memberTenant)).thenReturn(Optional.of(centralTenant));

    var captor = ArgumentCaptor.forClass(String.class);
    when(scopedExecutionService.executeSystemUserScoped(captor.capture(), any(Callable.class)))
        .thenAnswer(invocation -> {
          var argument = invocation.getArgument(1, Callable.class);
          return argument.call();
        });
    var result = consortiumTenantExecutor.executeAsCentralTenant(operation);

    assertNotNull(result);
    assertEquals(1, result);
    assertEquals(1, counter.get());
    assertEquals(centralTenant, captor.getValue());
  }
}
