DROP TABLE IF EXISTS
  city,
  person,
  str,
  lang;

CREATE TABLE city(id INT PRIMARY KEY, name VARCHAR(255));
CREATE TABLE person(id INT PRIMARY KEY, name VARCHAR(255), age INT, identifier UUID, is_alive boolean, date_born timestamp, local_date timestamp, city_id INT REFERENCES city(id), deleted boolean, created_date timestamp);
CREATE TABLE str(id INT PRIMARY KEY, str text);
CREATE TABLE lang(id INT PRIMARY KEY, name VARCHAR(255));
