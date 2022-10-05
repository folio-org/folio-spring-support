package org.folio.spring.cql.domain;

import java.util.Date;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import lombok.Data;

@Entity
@Data
public class Person {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;
  private String name;
  private int age;
  private UUID identifier;
  private Boolean isAlive;
  private Date dateBorn;
  private int count;
  private String cql;

  @ManyToOne
  @JoinColumn(name = "city_id")
  private City city;
}
