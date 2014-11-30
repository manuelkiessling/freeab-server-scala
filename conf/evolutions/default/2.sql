# --- !Ups

ALTER TABLE experiments ADD UNIQUE (name);

# --- !Downs

ALTER TABLE experiments DROP UNIQUE (name);
