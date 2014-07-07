# Lab schema
# --- !Ups

CREATE TABLE Lab (
    id      bigint NOT NULL AUTO_INCREMENT,
    acronym VARCHAR(255) NOT NULL,
    name    VARCHAR(500) NOT NULL,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE Lab;