# Creates the initial DB
# --- !Ups

CREATE TABLE Lab (
    id      INT UNSIGNED NOT NULL AUTO_INCREMENT,
    acronym VARCHAR(255) NOT NULL,
    name    VARCHAR(500) NOT NULL UNIQUE,
    email   VARCHAR(500) NOT NULL UNIQUE,
    logoId  VARCHAR(30) UNIQUE,
    PRIMARY KEY (id)
);

CREATE TABLE Speaker (
    id      INT UNSIGNED NOT NULL AUTO_INCREMENT,
    title     VARCHAR(20) NOT NULL,
    firstName VARCHAR(255) NOT NULL,
    lastName  VARCHAR(255) NOT NULL,
    email     VARCHAR(255) NOT NULL,
    team      VARCHAR(255) NOT NULL,
    organisation      VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE Location (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    instituteName VARCHAR(255) NOT NULL,
    buildingName  VARCHAR(255),
    roomDesignation VARCHAR(255) NOT NULL,
    floor         VARCHAR(255) NOT NULL,
    streetName    VARCHAR(255) NOT NULL,
    streetNb      INT NOT NULL,
    city          VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE AppUser (
    id        INT UNSIGNED NOT NULL AUTO_INCREMENT,
    firstName VARCHAR(255) NOT NULL,
    lastName  VARCHAR(255) NOT NULL,
    email     VARCHAR(255) NOT NULL,
    lab       INT UNSIGNED NOT NULL
        REFERENCES Lab(id)
            ON DELETE  RESTRICT,
    hashedPass VARCHAR(255) NOT NULL,
    role      int NOT NULL,
    rememberMeToken VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE Conference (
    id        INT UNSIGNED NOT NULL AUTO_INCREMENT,
    title     VARCHAR(255) NOT NULL,
    abstr     VARCHAR(255) NOT NULL,
    speaker   INT UNSIGNED NOT NULL REFERENCES Speaker(id),
    startDate TIMESTAMP NOT NULL,
    length    bigint NOT NULL, -- The duration of the conference in ms
    organizedBy INT UNSIGNED NOT NULL
        REFERENCES Lab(id)
            ON DELETE  RESTRICT,
    location  INT UNSIGNED NOT NULL
        REFERENCES Location(id)
            ON DELETE  RESTRICT,
    accepted  boolean NOT NULL,
    acceptCode VARCHAR(255),
    private   boolean NOT NULL,
	forGroup  INT UNSIGNED
	    REFERENCES LabGroup(id)
	        ON DELETE  RESTRICT,
	logoId  VARCHAR(30) UNIQUE,
    createdBy INT UNSIGNED DEFAULT NULL
        REFERENCES AppUser(id)
            ON DELETE CASCADE,
    PRIMARY KEY (id)
);

CREATE TABLE LabGroup (
    id      INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name    VARCHAR(500) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

CREATE TABLE IndexLabGroup (
	id_group      INT UNSIGNED NOT NULL,
	id_lab      INT UNSIGNED NOT NULL,
	FOREIGN KEY (id_group)
      REFERENCES LabGroup(id)
		ON DELETE CASCADE,
	FOREIGN KEY (id_lab)
      REFERENCES Lab(id)
		ON DELETE CASCADE
)

# --- !Downs
DROP TABLE IndexLabGroup;
DROP TABLE Conference;
DROP TABLE Speaker;
DROP TABLE Location;
DROP TABLE AppUser;
DROP TABLE LabGroup;
DROP TABLE Lab;