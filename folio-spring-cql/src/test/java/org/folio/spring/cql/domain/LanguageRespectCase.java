package org.folio.spring.cql.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.folio.spring.cql.RespectCase;

@Entity
@Data
@Table(name = "lang")
@RespectCase
public class LanguageRespectCase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;
  private String name;
}
