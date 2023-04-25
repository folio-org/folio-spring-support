DROP TABLE IF EXISTS usr_attribute;
DROP TABLE IF EXISTS usr;

CREATE TABLE usr (
  id UUID NOT NULL,
  name VARCHAR(255) NOT NULL,
  CONSTRAINT pk_usr PRIMARY KEY (id)
);

CREATE TABLE usr_attribute (
  id UUID NOT NULL,
   key VARCHAR(255) NOT NULL,
   str_value VARCHAR(255),
   int_value INTEGER,
   long_value BIGINT,
   bool_value BOOLEAN,
   uuid_value UUID,
   date_value TIMESTAMP WITHOUT TIME ZONE,
   user_id UUID,
   CONSTRAINT pk_usr_attribute PRIMARY KEY (id)
);

ALTER TABLE usr_attribute ADD CONSTRAINT FK_USR_ATTRIBUTE_ON_USR FOREIGN KEY (user_id) REFERENCES usr (id);
