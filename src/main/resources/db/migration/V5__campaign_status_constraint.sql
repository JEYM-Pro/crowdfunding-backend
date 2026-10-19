-- V5: Alinea la restricción de estados de campaigns con CampaignStatus.
-- Ejecutar en Supabase (SQL Editor); ddl-auto=validate no actualiza restricciones.

DO $$
DECLARE
    invalid_statuses TEXT;
BEGIN
    SELECT string_agg(DISTINCT coalesce(status, '<NULL>'), ', ' ORDER BY coalesce(status, '<NULL>'))
      INTO invalid_statuses
      FROM campaigns
     WHERE status IS NULL
        OR upper(status) NOT IN ('ACTIVE', 'PAUSED', 'CLOSED', 'FINISHED');

    IF invalid_statuses IS NOT NULL THEN
        RAISE EXCEPTION 'Estados de campaña no soportados: %', invalid_statuses;
    END IF;
END $$;

ALTER TABLE campaigns
    DROP CONSTRAINT IF EXISTS campaigns_status_check;

UPDATE campaigns
   SET status = upper(status);

ALTER TABLE campaigns
    ADD CONSTRAINT campaigns_status_check
    CHECK (status IN ('ACTIVE', 'PAUSED', 'CLOSED', 'FINISHED'));
