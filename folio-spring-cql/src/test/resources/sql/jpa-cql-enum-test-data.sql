-- Create PostgreSQL enum type
CREATE TYPE capability_type AS ENUM ('SETTINGS', 'DATA');

-- Create table with one enum column
CREATE TABLE capability_set (
  id UUID PRIMARY KEY,
  name VARCHAR(255),
  resource VARCHAR(255),
  type capability_type NOT NULL
);

-- Insert test data
INSERT INTO capability_set (id, name, resource, type) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Foo Item Manager', 'Foo Item', 'DATA'),
    ('22222222-2222-2222-2222-222222222222', 'Bar Item Viewer', 'Bar Item', 'DATA'),
    ('33333333-3333-3333-3333-333333333333', 'Foo Settings Manager', 'Foo Settings', 'SETTINGS')
