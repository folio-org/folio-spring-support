package org.folio.spring.cql;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;
import org.folio.spring.cql.domain.User;
import org.folio.spring.cql.repo.UserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;


@SpringBootTest
@AutoConfigureEmbeddedDatabase(beanName = "dataSource", type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@ContextConfiguration(classes = JpaCqlConfiguration.class)
@EnableAutoConfiguration(exclude = FlywayAutoConfiguration.class)
@Sql({"/sql/jpa-cql-modifiers-it-schema.sql", "/sql/jpa-cql-modifiers-test-data.sql"})
class JpaCqlModifiersIT {

  @Autowired
  private UserRepository userRepository;

  @ParameterizedTest
  @ValueSource(strings = {
    "name=user1 and attributes=/@key=key1/@stringValue=val1 *",
    "name=user1 and attributes=/@key=key1/@stringValue=val1 * and attributes=/@key=key2/@stringValue=val2 *",
    "name=user1 and (attributes=/@key=key3/@stringValue=val3 * or attributes=/@key=key2/@stringValue=val2 *)",
    "name=user1 and (attributes=/@key=key1/@stringValue=val1 * not attributes=/@key=key3/@stringValue=val3 *)",
    "name=user2 not attributes=/@key=key1/@stringValue=val1 *",
    "name=user1 not attributes=/@key=key3/@stringValue=val3 *",
    "name=user2 not (attributes=/@key=key1/@stringValue=val1 * and attributes=/@key=key3/@stringValue=val3 *)",
    "name=user2 not (attributes=/@key=key1/@stringValue=val1 * or attributes=/@key=key2/@stringValue=val2 *)",
    "name=user1 or attributes=/@key=key6/@stringValue=val6 *",
    "name=user3 or attributes=/@key=key1/@stringValue=val1 *",
    "name=user1 or (attributes=/@key=key6/@stringValue=val6 * or attributes=/@key=key1/@stringValue=val1 *)",
    "name=user3 or (attributes=/@key=key1/@stringValue=val1 * and attributes=/@key=key2/@stringValue=val2 *)",
    "attributes=/@key=key1 *"
  })
  void testBooleansForModifiers(String query) {
    var page = userRepository.findByCql(query, PageRequest.of(0, 10));
    assertThat(page).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "name=user1 and attributes=/@stringValue=val1 *",
    "name=user1 and attributes=/@intValue=1 *",
    "name=user1 and attributes=/@longValue=2 *",
    "name=user1 and attributes=/@boolValue=true *",
    "name=user1 and attributes=/@dateValue=\"2011-07-01T06:30:30\" *",
    "name=user1 and attributes=/@uuidValue=c330b021-9ef7-46b0-a8ed-200135bffe4b *",
  })
  void testFieldTypesForModifiers(String query) {
    var page = userRepository.findByCql(query, PageRequest.of(0, 10));
    assertThat(page).hasSize(1).extracting(User::getName).containsExactly("user1");
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "name=user1 and attributes=/@stringValue==val1 *",
    "name=user1 and attributes=/@stringValue<>val3 *",
    "name=user1 and attributes=/@intValue==1 *",
    "name=user1 and attributes=/@intValue<>3 *",
    "name=user1 and attributes=/@intValue<3 *",
    "name=user1 and attributes=/@intValue>0 *",
    "name=user1 and attributes=/@intValue>=1 *",
    "name=user1 and attributes=/@intValue<=1 *",
    "name=user1 and attributes=/@dateValue==\"2011-07-01T06:30:30\" *",
    "name=user1 and attributes=/@dateValue<=\"2011-07-01T06:30:30\" *",
    "name=user1 and attributes=/@dateValue>=\"2011-07-01T06:30:30\" *",
    "name=user1 and attributes=/@dateValue>\"2011-07-01T06:30:00\" *",
    "name=user1 and attributes=/@dateValue<\"2011-07-01T06:30:31\" *",
    "name=user1 and attributes=/@boolValue==true *",
    "name=user1 and attributes=/@boolValue<>false *",
    "name=user1 and attributes=/@uuidValue==c330b021-9ef7-46b0-a8ed-200135bffe4b *",
    "name=user1 and attributes=/@uuidValue<>13d84483-59f4-47cf-806a-2dc1296112ca *",
  })
  void testOperatorsForModifiers(String query) {
    var page = userRepository.findByCql(query, PageRequest.of(0, 10));
    assertThat(page).hasSize(1).extracting(User::getName).containsExactly("user1");
  }
}

