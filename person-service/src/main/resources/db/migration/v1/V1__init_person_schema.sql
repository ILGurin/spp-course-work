CREATE TABLE person.users
(
    id         UUID PRIMARY KEY                     DEFAULT uuid_generate_v4(),
    active     boolean                     NOT NULL DEFAULT TRUE,
    created    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    email      VARCHAR(1024)               NOT NULL,
    first_name VARCHAR(64)                 NOT NULL,
    last_name  VARCHAR(64)                 NOT NULL
);

CREATE TABLE person.individuals
(
    id              UUID PRIMARY KEY                     DEFAULT uuid_generate_v4(),
    active          boolean                     NOT NULL DEFAULT TRUE,
    created         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    passport_number VARCHAR(64)                 NOT NULL,
    phone_number    VARCHAR(64)                 NOT NULL,
    user_id         UUID                        NOT NULL REFERENCES person.users (id)
);