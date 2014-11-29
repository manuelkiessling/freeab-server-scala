# --- !Ups

CREATE TABLE experiments (
    id int SERIAL PRIMARY KEY,
    name varchar NOT NULL,
    scope float NOT NULL);

# --- !Downs

DROP TABLE IF EXISTS experiments;
