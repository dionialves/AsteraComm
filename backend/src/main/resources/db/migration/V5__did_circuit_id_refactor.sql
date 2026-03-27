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
