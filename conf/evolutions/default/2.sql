# Speaker schema
# --- !Ups

CREATE TABLE Speaker (
    id      bigint NOT NULL AUTO_INCREMENT,
    title     VARCHAR(20) NOT NULL,
    firstName VARCHAR(255) NOT NULL,
    lastName  VARCHAR(255) NOT NULL,
    email     VARCHAR(255) NOT NULL,
    team      VARCHAR(255) NOT NULL,
    organisation      VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE Speaker;