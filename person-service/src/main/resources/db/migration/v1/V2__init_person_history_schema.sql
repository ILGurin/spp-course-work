CREATE TABLE person_history.revinfo
(
    rev       BIGSERIAL PRIMARY KEY,
    revtmstmp BIGINT
);

CREATE TABLE person_history.users_history
(
    id            UUID                        NOT NULL,
    revision      BIGINT                      NOT NULL,
    revision_type SMALLINT                    NOT NULL,
    active        BOOLEAN                     NOT NULL DEFAULT TRUE,
    created       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    email         VARCHAR(1024)               NOT NULL,
    first_name    VARCHAR(64)                 NOT NULL,
    last_name     VARCHAR(64)                 NOT NULL,

    CONSTRAINT pk_users_history PRIMARY KEY (id, revision),
    CONSTRAINT fk_users_history_rev FOREIGN KEY (revision) REFERENCES person_history.revinfo (rev)
);

CREATE INDEX IF NOT EXISTS idx_users_history_revision ON person_history.users_history (revision);

CREATE TABLE person_history.individuals_history
(
    id              UUID                        NOT NULL,
    revision        BIGINT                      NOT NULL,
    revision_type   SMALLINT                    NOT NULL,
    active          BOOLEAN                     NOT NULL DEFAULT TRUE,
    created         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    phone_number    VARCHAR(64)                 NOT NULL,
    user_id         UUID                        NOT NULL,

    CONSTRAINT pk_individuals_history PRIMARY KEY (id, revision),
    CONSTRAINT fk_individuals_history_rev FOREIGN KEY (revision) REFERENCES person_history.revinfo (rev)
);

CREATE INDEX IF NOT EXISTS idx_individuals_history_revision ON person_history.individuals_history (revision);