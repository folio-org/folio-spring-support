package org.folio.spring.support;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration(exclude = FlywayAutoConfiguration.class)
public class DbConfiguration {
  @Bean
  DataSource dataSource() {
    return DataSourceBuilder.create().build();
  }
}
