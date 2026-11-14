-- V9: Contraseña de campaña (HU-S02, HU-S03 y HU-S04)
-- Cada campaña guarda el hash BCrypt de su PIN de 6 dígitos (nunca el PIN en claro).
-- Las campañas que ya existían reciben el PIN por defecto 123456 (acordado con el equipo);
-- las nuevas lo definen al publicarse. Idempotente: se puede ejecutar más de una vez.

ALTER TABLE campaigns
    ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(100);

UPDATE campaigns
   SET pin_hash = '$2a$10$3TlrtQdKHWpqWpwZah39AusajN44G9MSwpBESyK0r81zQOcmYPVvi'
 WHERE pin_hash IS NULL;

ALTER TABLE campaigns
    ALTER COLUMN pin_hash SET NOT NULL;

COMMENT ON COLUMN campaigns.pin_hash IS 'Hash BCrypt de la contraseña de campaña (PIN de 6 dígitos)';
