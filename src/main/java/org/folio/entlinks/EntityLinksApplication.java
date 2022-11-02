package org.folio.entlinks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class EntityLinksApplication {

  public static void main(String[] args) {
    SpringApplication.run(EntityLinksApplication.class, args);
  }

}
