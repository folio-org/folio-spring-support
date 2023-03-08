package org.folio.spring.cql.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "str")
public class Str {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;
  private String str;
}
