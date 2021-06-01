DROP TABLE IF EXISTS person;
DROP TABLE IF EXISTS city;

CREATE TABLE city(id INT PRIMARY KEY, name VARCHAR(255));
CREATE TABLE person(id INT PRIMARY KEY, name VARCHAR(255), age INT, identifier UUID, is_alive boolean, date_born timestamp, city_id INT REFERENCES city(id));
