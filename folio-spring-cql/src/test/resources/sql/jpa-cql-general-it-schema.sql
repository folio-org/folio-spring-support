CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;
CREATE OR REPLACE FUNCTION f_unaccent(text) RETURNS text AS $$
  SELECT public.unaccent('public.unaccent', $1)
$$ LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT;

DROP TABLE IF EXISTS
  city,
  person,
  str,
  lang;

DROP TYPE IF EXISTS PersonStatus;
CREATE TYPE PersonStatus AS ENUM ('RESIDENT', 'IMMIGRANT');

CREATE TABLE city(id INT PRIMARY KEY, name VARCHAR(255));
CREATE TABLE person(id INT PRIMARY KEY, name VARCHAR(255), age INT, identifier UUID, is_alive boolean, date_born timestamp, status PersonStatus, local_date timestamp, city_id INT REFERENCES city(id), deleted boolean, created_date timestamp);
CREATE TABLE str(id INT PRIMARY KEY, str text);
CREATE TABLE lang(id INT PRIMARY KEY, name VARCHAR(255));
