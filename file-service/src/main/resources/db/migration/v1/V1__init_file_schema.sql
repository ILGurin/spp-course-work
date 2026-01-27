CREATE TABLE file.directories
(
    id        UUID PRIMARY KEY                     DEFAULT uuid_generate_v4(),
    active    boolean                     NOT NULL DEFAULT TRUE,
    created   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    user_id   UUID                        NOT NULL,
    parent_id UUID                        NULL REFERENCES file.directories (id),
    name      VARCHAR(255)                NOT NULL,
    path      VARCHAR(2048)               NULL
);

CREATE TABLE file.files
(
    id           UUID PRIMARY KEY                     DEFAULT uuid_generate_v4(),
    active       boolean                     NOT NULL DEFAULT TRUE,
    created      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    user_id      UUID                        NOT NULL,
    directory_id UUID                        NOT NULL REFERENCES file.directories (id),
    file_name    VARCHAR(255)                 NOT NULL,
    object_name  VARCHAR(64)                 NOT NULL,
    file_size    INTEGER                     NOT NULL,
    mime_type    VARCHAR(128)                 NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_files_user_id ON file.files (user_id);
CREATE INDEX IF NOT EXISTS idx_files_directory_id ON file.files (directory_id);
CREATE INDEX IF NOT EXISTS idx_directories_user_id ON file.directories (user_id);
CREATE INDEX IF NOT EXISTS idx_directories_parent_id ON file.directories (parent_id);

