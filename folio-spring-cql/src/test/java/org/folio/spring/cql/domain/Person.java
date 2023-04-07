package org.folio.spring.cql.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Entity
@Data
@Table(name = "person")
public class Person {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;
  private String name;
  private int age;
  private UUID identifier;
  private Boolean isAlive;
  private Date dateBorn;
  private LocalDateTime localDate;

  @ManyToOne
  @JoinColumn(name = "city_id")
  private City city;
}
