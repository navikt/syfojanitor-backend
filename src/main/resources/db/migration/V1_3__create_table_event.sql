CREATE TABLE EVENT
(
    id                 SERIAL PRIMARY KEY,
    uuid               CHAR(36)    NOT NULL UNIQUE,
    reference_uuid     CHAR(36)    NOT NULL,
    personident        CHAR(11)    NOT NULL,
    navident           CHAR(7)     NOT NULL,
    created_at         timestamptz NOT NULL,
    updated_at         timestamptz NOT NULL,
    type               TEXT        NOT NULL,
    description        TEXT,
    status             TEXT        NOT NULL
)
