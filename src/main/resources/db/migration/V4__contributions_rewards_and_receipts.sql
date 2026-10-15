-- V4: HU-11 (nivel de recompensa) y HU-12 (confirmación de contribución)
-- Ejecutar en Supabase (SQL Editor) ANTES de levantar el backend: ddl-auto=validate.
-- Es idempotente: se puede ejecutar más de una vez sin romper nada.

-- 1) Una recompensa agotada tiene stock = 0 (V3 solo permitía stock > 0).
--    Se elimina el CHECK actual sobre stock (sea cual sea su nombre) y se crea uno que admite 0.
DO $$
DECLARE
    c RECORD;
BEGIN
    FOR c IN
        SELECT conname
          FROM pg_constraint
         WHERE conrelid = 'rewards'::regclass
           AND contype = 'c'
           AND pg_get_constraintdef(oid) ILIKE '%stock%'
    LOOP
        EXECUTE format('ALTER TABLE rewards DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

ALTER TABLE rewards ADD CONSTRAINT rewards_stock_check CHECK (stock >= 0);

-- 2) La transacción guarda la recompensa elegida (opcional) y una copia histórica de su nombre.
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS reward_id   UUID REFERENCES rewards (id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS reward_name VARCHAR(150);

CREATE INDEX IF NOT EXISTS idx_transactions_reward ON transactions (reward_id);
