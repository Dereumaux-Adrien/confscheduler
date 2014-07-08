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

CREATE TABLE User (
    id        bigint NOT NULL AUTO_INCREMENT,
    firstName VARCHAR(255) NOT NULL,
    lastName  VARCHAR(255) NOT NULL,
    email     VARCHAR(255) NOT NULL,
    lab       bigint NOT NULL REFERENCES Lab(id),
    hashedPass VARCHAR(255),
    role      smallint,
    rememberMeToken VARCHAR(255),
    PRIMARY KEY (id)
)

# --- !Downs

DROP TABLE User;
DROP Table Conference;
DROP TABLE Lab;
DROP TABLE Speaker;

