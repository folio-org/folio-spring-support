package org.folio.spring.cql.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "grp")
public class Group {

  @Id
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "grp_member", joinColumns = @JoinColumn(name = "group_id"))
  @Column(name = "member_id")
  private List<UUID> memberIds = new ArrayList<>();
}
