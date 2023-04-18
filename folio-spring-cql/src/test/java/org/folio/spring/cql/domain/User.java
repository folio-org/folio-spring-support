package org.folio.spring.cql.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Data
@Entity
@Table(name = "usr")
public class User {

  @Id
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @OrderBy("key")
  @Fetch(FetchMode.SUBSELECT)
  @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER,
    mappedBy = "user",
    orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<UserAttribute> attributes = new ArrayList<>();
}
