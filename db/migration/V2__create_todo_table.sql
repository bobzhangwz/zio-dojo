
CREATE TABLE todos (
  id SERIAL PRIMARY KEY,
  orders SMALLINT NOT NULL,
  completed BOOLEAN NOT NULL DEFAULT false,
  url varchar(1024)
);

CREATE INDEX order_index ON todos (orders);
