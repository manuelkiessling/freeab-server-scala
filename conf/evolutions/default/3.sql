# --- !Ups

CREATE TABLE variations (
    id int SERIAL PRIMARY KEY,
    experiment_id int,
    name varchar NOT NULL,
    weight float NOT NULL);

CREATE INDEX experiment_id ON variations(experiment_id);

# --- !Downs

DROP TABLE IF EXISTS variations;
