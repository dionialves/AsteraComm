-- US-067: suporte a troncos por autenticação de IP
ALTER TABLE asteracomm_trunks
    ADD COLUMN auth_type     VARCHAR(20)  NOT NULL DEFAULT 'CREDENTIAL',
    ADD COLUMN identify_match VARCHAR(255),
    ALTER COLUMN username    DROP NOT NULL,
    ALTER COLUMN password    DROP NOT NULL;
