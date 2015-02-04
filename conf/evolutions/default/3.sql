# --- !Ups

CREATE TABLE variations (
    id varchar PRIMARY KEY,
    experiment_id varchar NOT NULL,
    name varchar NOT NULL,
    weight float NOT NULL);

CREATE INDEX variations_experiment_id ON variations (experiment_id);

# --- !Downs

DROP TABLE IF EXISTS variations;
