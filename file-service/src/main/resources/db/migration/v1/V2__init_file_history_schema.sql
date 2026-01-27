CREATE TABLE file_history.revinfo
(
    rev       BIGSERIAL PRIMARY KEY,
    revtmstmp BIGINT
);

CREATE TABLE file_history.files_history
(
    id            UUID                        NOT NULL,
    revision      BIGINT                      NOT NULL,
    revision_type SMALLINT                    NOT NULL,
    active        BOOLEAN                     NOT NULL DEFAULT TRUE,
    created       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    user_id       UUID                        NOT NULL,
    directory_id  UUID                        NOT NULL,
    file_name     VARCHAR(255)                 NOT NULL,
    object_name   VARCHAR(64)                 NOT NULL,
    file_size     INTEGER                     NOT NULL,
    mime_type     VARCHAR(128)                 NOT NULL,

    CONSTRAINT pk_files_history PRIMARY KEY (id, revision),
    CONSTRAINT fk_files_history_rev FOREIGN KEY (revision) REFERENCES file_history.revinfo (rev)
);

CREATE INDEX IF NOT EXISTS idx_files_history_revision ON file_history.files_history (revision);

CREATE TABLE file_history.directories_history
(
    id            UUID                        NOT NULL,
    revision      BIGINT                      NOT NULL,
    revision_type SMALLINT                    NOT NULL,
    active        BOOLEAN                     NOT NULL DEFAULT TRUE,
    created       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    user_id       UUID                        NOT NULL,
    parent_id     UUID                        NULL,
    name          VARCHAR(255)                NOT NULL,
    path          VARCHAR(2048)               NULL,

    CONSTRAINT pk_directories_history PRIMARY KEY (id, revision),
    CONSTRAINT fk_directories_history_rev FOREIGN KEY (revision) REFERENCES file_history.revinfo (rev)
);

CREATE INDEX IF NOT EXISTS idx_directories_history_revision ON file_history.directories_history (revision);

