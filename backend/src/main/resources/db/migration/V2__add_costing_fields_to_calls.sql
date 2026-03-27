ALTER TABLE asteracomm_calls
    ADD COLUMN call_status        VARCHAR(30),
    ADD COLUMN minutes_from_quota INTEGER,
    ADD COLUMN cost               NUMERIC(10, 2);
