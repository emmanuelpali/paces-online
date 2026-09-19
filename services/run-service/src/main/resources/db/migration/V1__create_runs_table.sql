CREATE TABLE runs
(
    id                                      UUID         PRIMARY KEY,
    user_id                                 UUID         NOT NULL,
    started_at                              TIMESTAMPTZ  NOT NULL,
    run_type                                VARCHAR(20)  NOT NULL,
    distance_kilometres                     NUMERIC(7,3) NOT NULL,
    duration_seconds                        INTEGER      NOT NULL,
    average_pace_seconds_per_kilometre       INTEGER      NOT NULL,
    perceived_effort                        SMALLINT,
    notes                                   VARCHAR(1000),
    created_at                              TIMESTAMPTZ  NOT NULL,
    updated_at                              TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_runs_run_type
        CHECK (run_type IN (
            'EASY',
            'RECOVERY',
            'LONG',
            'TEMPO',
            'INTERVAL',
            'HILL',
            'FARTLEK',
            'RACE',
            'OTHER'
        )),

    CONSTRAINT chk_runs_distance_positive
        CHECK (distance_kilometres > 0),

    CONSTRAINT chk_runs_duration_positive
        CHECK (duration_seconds > 0),

    CONSTRAINT chk_runs_average_pace_positive
        CHECK (average_pace_seconds_per_kilometre > 0),

    CONSTRAINT chk_runs_perceived_effort_range
        CHECK (perceived_effort BETWEEN 1 AND 10)
);