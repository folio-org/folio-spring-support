package org.folio.spring.cql.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
@Entity
@Table(name = "usr_attribute")
public class UserAttribute {

  public static final Sort SORT_BY_KEY = Sort.by(Sort.Direction.ASC, "key");

  @Id
  private UUID id;

  @Column(name = "key", nullable = false)
  private String key;

  @Column(name = "str_value")
  private String stringValue;

  @Column(name = "int_value")
  private Integer intValue;

  @Column(name = "long_value")
  private Long longValue;

  @Column(name = "bool_value")
  private Boolean boolValue;

  @Column(name = "uuid_value")
  private UUID uuidValue;

  @Column(name = "date_value")
  private LocalDateTime dateValue;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;
}
