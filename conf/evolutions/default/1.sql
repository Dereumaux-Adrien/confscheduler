# Lab schema
# --- !Ups

CREATE TABLE Lab (
    id      bigint NOT NULL AUTO_INCREMENT,
    acronym VARCHAR(255) NOT NULL UNIQUE,
    name    VARCHAR(500) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE Lab;