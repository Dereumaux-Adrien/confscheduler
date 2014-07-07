# Creates the initial DB
# --- !Ups

CREATE TABLE Lab (
    id      bigint NOT NULL AUTO_INCREMENT,
    acronym VARCHAR(255) NOT NULL UNIQUE,
    name    VARCHAR(500) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

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

CREATE TABLE Conference (
    id        bigint NOT NULL AUTO_INCREMENT,
    title     VARCHAR(255) NOT NULL,
    abstr     VARCHAR(255) NOT NULL,
    speaker   bigint NOT NULL REFERENCES Speaker(id),
    startDate TIMESTAMP NOT NULL,
    length    bigint NOT NULL, -- The duration of the conference in ms
    organizedBy      bigint NOT NULL REFERENCES Lab(id),
    accepted  boolean,
    PRIMARY KEY (id)
);

# --- !Downs

DROP Table Conference;
DROP TABLE Lab;
DROP TABLE Speaker;

