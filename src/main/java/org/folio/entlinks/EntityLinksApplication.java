package org.folio.entlinks;

import java.util.concurrent.Executor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@EnableKafka
@EnableCaching
@SpringBootApplication
public class EntityLinksApplication {

  public static void main(String[] args) {
    SpringApplication.run(EntityLinksApplication.class, args);
  }

  @Bean(name = "threadPoolTaskExecutor")
  public Executor threadPoolTaskExecutor() {
    var threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(10);
    threadPoolTaskExecutor.setMaxPoolSize(10);
    return threadPoolTaskExecutor;
  }

}
