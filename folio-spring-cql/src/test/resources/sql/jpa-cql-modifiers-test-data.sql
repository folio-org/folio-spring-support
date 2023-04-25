INSERT INTO usr(id, name)
VALUES ('3e9d549f-7d42-4b71-8f8b-06d8e78d695a', 'user1');

INSERT INTO usr(id, name)
VALUES ('29fce73c-06c6-4680-ba14-de1560d68efd', 'user2');

INSERT INTO usr_attribute(id, KEY, str_value, int_value, long_value, bool_value, uuid_value, date_value, user_id)
VALUES ('606241f9-bdbc-4b5e-91b5-6d93e9c8dbb7', 'key1', 'val1', 1, 2, TRUE, 'c330b021-9ef7-46b0-a8ed-200135bffe4b', '2011-07-01 06:30:30', '3e9d549f-7d42-4b71-8f8b-06d8e78d695a');

INSERT INTO usr_attribute(id, KEY, str_value, int_value, long_value, bool_value, uuid_value, date_value, user_id)
VALUES ('6626e507-f1b3-4e2e-b0e7-7b3fbab99c07', 'key2', 'val2', 3, 4, FALSE, '4236b7cd-46a9-4d2c-a5f6-572d28b87bea', '2012-07-01 06:30:30', '3e9d549f-7d42-4b71-8f8b-06d8e78d695a');

INSERT INTO usr_attribute(id, KEY, str_value, int_value, long_value, bool_value, uuid_value, date_value, user_id)
VALUES ('c09ee2b1-3067-4afe-8737-ab3a61dfbedc', 'key3', 'val3', 5, 6, FALSE, 'acc60de9-caf6-4c5e-b6e4-5e8ad9dca35c', '2013-07-01 06:30:30', '29fce73c-06c6-4680-ba14-de1560d68efd');
