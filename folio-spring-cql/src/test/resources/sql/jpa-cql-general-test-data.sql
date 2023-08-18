insert into city(id, name) values (1, 'Kharkiv');
insert into city(id, name) values (2, 'Kyiv');

insert into person(id, name, age, city_id, date_born, local_date, deleted) values (101, 'Jane', 20, 1, '2001-01-03', '2001-01-03', false);
insert into person(id, name, age, city_id, date_born, local_date, deleted) values (102, 'John', 22, 2, '2001-01-01', '2001-01-01', false);
insert into person(id, name, age, city_id, date_born, local_date, deleted) values (103, 'John', 40, 1, '2001-01-02', '2001-01-02', false);

insert into str(id, str) values
  (1, 'a'),
  (2, 'ab'),
  (3, 'abc'),
  (4, '*'),
  (5, '?'),
  (6, '%'),
  (7, '_'),
  (8, '\'),
  (9, ''''),
  (10, '"');

