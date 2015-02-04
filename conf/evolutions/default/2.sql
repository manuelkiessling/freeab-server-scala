# --- !Ups

CREATE UNIQUE INDEX experiments_name ON experiments (name);

# --- !Downs

DROP INDEX experiments_name;