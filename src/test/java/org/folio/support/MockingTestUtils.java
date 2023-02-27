package org.folio.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import org.folio.spring.tools.batch.MessageBatchProcessor;

@UtilityClass
public class MockingTestUtils {

  @SuppressWarnings("unchecked")
  public static void mockBatchSuccessHandling(MessageBatchProcessor messageBatchProcessor) {
    doAnswer(invocation -> {
      var argument = invocation.getArgument(2, Consumer.class);
      var batch = invocation.getArgument(0, List.class);
      argument.accept(batch);
      return null;
    }).when(messageBatchProcessor).consumeBatchWithFallback(any(), any(), any(), any());
  }

  @SuppressWarnings("unchecked")
  public static void mockBatchFailedHandling(MessageBatchProcessor messageBatchProcessor, Exception e) {
    doAnswer(invocation -> {
      var argument = invocation.getArgument(3, BiConsumer.class);
      var batch = invocation.getArgument(0, List.class);
      argument.accept(batch.get(0), e);
      return null;
    }).when(messageBatchProcessor).consumeBatchWithFallback(any(), any(), any(), any());
  }
}
