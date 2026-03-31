-- FIX-068: minutes_from_quota armazenava frações de 30s (int) em vez de minutos reais (numeric).
-- Converte registros existentes dividindo por 2: ex. 15 frações → 7.5 minutos.
ALTER TABLE asteracomm_calls
    ALTER COLUMN minutes_from_quota TYPE NUMERIC(10, 1)
        USING minutes_from_quota / 2.0;
