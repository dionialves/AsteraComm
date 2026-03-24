CREATE TABLE IF NOT EXISTS asteracomm_users (
  id         BIGSERIAL PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  username   VARCHAR(100) NOT NULL UNIQUE,
  password   VARCHAR(255) NOT NULL,
  role       VARCHAR(30) NOT NULL,
  enabled    BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);


CREATE TABLE IF NOT EXISTS asteracomm_trunks (
  name VARCHAR(40) PRIMARY KEY,
  host VARCHAR(255) NOT NULL,
  username VARCHAR(80) NOT NULL,
  password VARCHAR(80) NOT NULL,
  prefix VARCHAR(20)
);


CREATE TABLE IF NOT EXISTS asteracomm_trunk_registration_status (
  id BIGSERIAL PRIMARY KEY,
  trunk_name VARCHAR(40) NOT NULL,
  registered BOOLEAN NOT NULL,
  checked_at TIMESTAMP NOT NULL
);


CREATE TABLE IF NOT EXISTS asteracomm_customers (
  id         BIGSERIAL PRIMARY KEY,
  name       VARCHAR(100) NOT NULL UNIQUE,
  enabled    BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS asteracomm_plans (
  id                           BIGSERIAL PRIMARY KEY,
  name                         VARCHAR(100) NOT NULL UNIQUE,
  monthly_price                NUMERIC(10,2) NOT NULL CHECK (monthly_price >= 0),
  fixed_local                  NUMERIC(10,4) NOT NULL CHECK (fixed_local >= 0),
  fixed_long_distance          NUMERIC(10,4) NOT NULL CHECK (fixed_long_distance >= 0),
  mobile_local                 NUMERIC(10,4) NOT NULL CHECK (mobile_local >= 0),
  mobile_long_distance         NUMERIC(10,4) NOT NULL CHECK (mobile_long_distance >= 0),
  package_type                 VARCHAR(20) NOT NULL DEFAULT 'NONE',
  package_total_minutes        INTEGER CHECK (package_total_minutes > 0),
  package_fixed_local          INTEGER CHECK (package_fixed_local >= 0),
  package_fixed_long_distance  INTEGER CHECK (package_fixed_long_distance >= 0),
  package_mobile_local         INTEGER CHECK (package_mobile_local >= 0),
  package_mobile_long_distance INTEGER CHECK (package_mobile_long_distance >= 0),
  created_at                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS asteracomm_circuits (
  id          BIGSERIAL PRIMARY KEY,
  number      VARCHAR(20) NOT NULL UNIQUE,
  password    VARCHAR(80) NOT NULL,
  trunk_name  VARCHAR(40) NOT NULL,
  customer_id BIGINT NOT NULL REFERENCES asteracomm_customers(id),
  plan_id     BIGINT NOT NULL REFERENCES asteracomm_plans(id)
);


CREATE TABLE IF NOT EXISTS asteracomm_endpoint_status (
  id BIGSERIAL PRIMARY KEY,
  endpoint VARCHAR(40) NOT NULL REFERENCES ps_endpoints(id),
  online BOOLEAN NOT NULL,
  ip VARCHAR(45),
  rtt VARCHAR(20),
  checked_at TIMESTAMP NOT NULL
);


CREATE TABLE IF NOT EXISTS asteracomm_dids (
  id BIGSERIAL PRIMARY KEY,
  number VARCHAR(10) NOT NULL UNIQUE,
  circuit_number VARCHAR(20) REFERENCES asteracomm_circuits(number)
);


CREATE TABLE IF NOT EXISTS asteracomm_calls (
  id               BIGSERIAL PRIMARY KEY,
  unique_id        VARCHAR(32) NOT NULL UNIQUE,
  call_date        TIMESTAMP NOT NULL,
  caller_number    VARCHAR(80),
  dst              VARCHAR(80) NOT NULL,
  duration_seconds INTEGER NOT NULL,
  bill_seconds     INTEGER NOT NULL,
  disposition      VARCHAR(45) NOT NULL,
  call_type        VARCHAR(30) NOT NULL,
  processed_at     TIMESTAMP NOT NULL,
  circuit_number   VARCHAR(20) REFERENCES asteracomm_circuits(number) ON DELETE SET NULL
);



ALTER TABLE asteracomm_calls
    ADD COLUMN call_status        VARCHAR(30),
    ADD COLUMN minutes_from_quota INTEGER,
    ADD COLUMN cost               NUMERIC(10, 2);

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

ALTER TABLE asteracomm_circuits
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;





-- Adiciona a nova coluna circuit_id
ALTER TABLE asteracomm_dids
    ADD COLUMN circuit_id BIGINT REFERENCES asteracomm_circuits(id);

-- Migra dados existentes: resolve o número do circuito para o id correspondente
UPDATE asteracomm_dids d
SET circuit_id = (
    SELECT c.id
    FROM asteracomm_circuits c
    WHERE c.number = d.circuit_number
)
WHERE d.circuit_number IS NOT NULL;

-- Remove a coluna antiga
ALTER TABLE asteracomm_dids
    DROP COLUMN circuit_number;






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




ALTER TABLE asteracomm_plans ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;



UPDATE asteracomm_users SET role = 'ADMIN' WHERE role IN ('SUPER_ADMIN', 'USER');

