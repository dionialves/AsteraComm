-- Drop FK constraints referencing circuits(number) so we can restructure
ALTER TABLE asteracomm_dids  DROP CONSTRAINT IF EXISTS asteracomm_dids_circuit_number_fkey;
ALTER TABLE asteracomm_calls DROP CONSTRAINT IF EXISTS asteracomm_calls_circuit_number_fkey;

-- Add sequential id column (no-op on fresh installs where 02-initial-flyway already added it)
ALTER TABLE asteracomm_circuits ADD COLUMN IF NOT EXISTS id BIGSERIAL;

-- Swap PK from number to id, only if PK is still on number (existing databases)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.key_column_usage kcu
        JOIN information_schema.table_constraints tc
          ON tc.constraint_name = kcu.constraint_name
         AND tc.table_name      = kcu.table_name
        WHERE tc.table_name      = 'asteracomm_circuits'
          AND tc.constraint_type = 'PRIMARY KEY'
          AND kcu.column_name    = 'number'
    ) THEN
        ALTER TABLE asteracomm_circuits DROP CONSTRAINT asteracomm_circuits_pkey;
        ALTER TABLE asteracomm_circuits ADD PRIMARY KEY (id);
        ALTER TABLE asteracomm_circuits ADD CONSTRAINT uq_circuits_number UNIQUE (number);
    END IF;
END $$;

-- Restore FK constraints
ALTER TABLE asteracomm_dids ADD CONSTRAINT asteracomm_dids_circuit_number_fkey
    FOREIGN KEY (circuit_number) REFERENCES asteracomm_circuits(number);

ALTER TABLE asteracomm_calls ADD CONSTRAINT asteracomm_calls_circuit_number_fkey
    FOREIGN KEY (circuit_number) REFERENCES asteracomm_circuits(number) ON DELETE SET NULL;
