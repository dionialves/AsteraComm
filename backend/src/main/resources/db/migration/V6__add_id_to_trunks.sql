-- Add sequential id column
ALTER TABLE asteracomm_trunks ADD COLUMN IF NOT EXISTS id BIGSERIAL;

-- Swap PK from name to id, only if PK is still on name (existing databases)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.key_column_usage kcu
        JOIN information_schema.table_constraints tc
          ON tc.constraint_name = kcu.constraint_name
         AND tc.table_name      = kcu.table_name
        WHERE tc.table_name      = 'asteracomm_trunks'
          AND tc.constraint_type = 'PRIMARY KEY'
          AND kcu.column_name    = 'name'
    ) THEN
        ALTER TABLE asteracomm_trunks DROP CONSTRAINT asteracomm_trunks_pkey;
        ALTER TABLE asteracomm_trunks ADD PRIMARY KEY (id);
        ALTER TABLE asteracomm_trunks ADD CONSTRAINT uq_trunks_name UNIQUE (name);
    END IF;
END $$;
