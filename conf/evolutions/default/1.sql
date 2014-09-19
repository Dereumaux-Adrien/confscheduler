# Creates the initial DB
# --- !Ups

CREATE TABLE Lab (
    id      SERIAL,
    acronym VARCHAR(255) NOT NULL UNIQUE,
    name    VARCHAR(500) NOT NULL UNIQUE,
    logoId  VARCHAR(30) UNIQUE,
    PRIMARY KEY (id)
);

CREATE TABLE Speaker (
    id      SERIAL,
    title     VARCHAR(20) NOT NULL,
    firstName VARCHAR(255) NOT NULL,
    lastName  VARCHAR(255) NOT NULL,
    email     VARCHAR(255) NOT NULL,
    team      VARCHAR(255) NOT NULL,
    organisation      VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE Location (
    id SERIAL,
    instituteName VARCHAR(255) NOT NULL,
    buildingName  VARCHAR(255),
    roomDesignation VARCHAR(255) NOT NULL,
    floor         VARCHAR(255) NOT NULL,
    streetName    VARCHAR(255) NOT NULL,
    streetNb      int NOT NULL,
    city          VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE Conference (
    id        SERIAL,
    title     VARCHAR(255) NOT NULL,
    abstr     VARCHAR(255) NOT NULL,
    speaker   bigint NOT NULL REFERENCES Speaker(id),
    startDate TIMESTAMP NOT NULL,
    length    bigint NOT NULL, -- The duration of the conference in ms
    organizedBy      bigint NOT NULL REFERENCES Lab(id),
    location  bigint NOT NULL REFERENCES Location(id),
    accepted  boolean NOT NULL,
    acceptCode VARCHAR(255),
    private   boolean NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE AppUser (
    id        SERIAL,
    firstName VARCHAR(255) NOT NULL,
    lastName  VARCHAR(255) NOT NULL,
    email     VARCHAR(255) NOT NULL,
    lab       bigint NOT NULL REFERENCES Lab(id),
    hashedPass VARCHAR(255) NOT NULL,
    role      int NOT NULL,
    rememberMeToken VARCHAR(255),
    PRIMARY KEY (id)
)

# --- !Downs

DROP TABLE AppUser;
DROP TABLE Conference;
DROP TABLE Location;
DROP TABLE Lab;
DROP TABLE Speaker;


