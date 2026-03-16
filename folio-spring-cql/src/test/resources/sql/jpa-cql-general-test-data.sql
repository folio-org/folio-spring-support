insert into city(id, name) values (1, 'Kharkiv');
insert into city(id, name) values (2, 'Kyiv');

insert into person(id, name, age, city_id, date_born, local_date, deleted, created_date) values (101, 'Jane', 20, 1, '2001-01-03', '2001-01-03', false, '2021-12-20T06:31:31+05:00');
insert into person(id, name, age, city_id, date_born, local_date, deleted, created_date) values (102, 'John', 22, 2, '2001-01-01', '2001-01-01', false, '2021-12-25T06:31:31+05:00');
insert into person(id, name, age, city_id, date_born, local_date, deleted, created_date) values (103, 'John', 40, 1, '2001-01-02', '2001-01-02', false, '2021-12-31T06:31:31+05:00');

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

insert into grp(id, name) values
  ('11111111-1111-1111-1111-111111111111', 'Admins'),
  ('22222222-2222-2222-2222-222222222222', 'Editors'),
  ('33333333-3333-3333-3333-333333333333', 'Empty Group');

insert into grp_member(group_id, member_id) values
  ('11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
  ('11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
  ('22222222-2222-2222-2222-222222222222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');

