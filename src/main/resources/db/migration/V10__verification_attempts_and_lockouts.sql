-- V10: Límite de intentos fallidos de verificación (HU-S07)
-- verification_attempts: contador de fallos consecutivos por usuario y por verificación
--   (scope = 'ACCOUNT' para la contraseña de la cuenta, o 'CAMPAIGN:<id>' para el PIN de una campaña).
-- verification_lockouts: registro de cada bloqueo con fecha, usuario y campaña involucrada.
--
-- Sin claves foráneas a propósito: los intentos se guardan en una transacción aparte mientras el aporte
-- tiene bloqueada la fila del usuario (SELECT ... FOR UPDATE); una FK a users obligaría a Postgres a
-- esperar ese bloqueo y la petición quedaría colgada. Igual que la tabla notifications (V7).
-- Idempotente: se puede ejecutar más de una vez.

CREATE TABLE IF NOT EXISTS verification_attempts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    scope           VARCHAR(60) NOT NULL,
    failed_attempts INTEGER     NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_verification_attempts_user_scope UNIQUE (user_id, scope)
);

CREATE TABLE IF NOT EXISTS verification_lockouts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    campaign_id     UUID,
    scope           VARCHAR(60) NOT NULL,
    failed_attempts INTEGER     NOT NULL,
    locked_at       TIMESTAMPTZ NOT NULL,
    locked_until    TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_verification_lockouts_user ON verification_lockouts (user_id, locked_at DESC);
CREATE INDEX IF NOT EXISTS idx_verification_lockouts_campaign ON verification_lockouts (campaign_id);
