-- V3: HU-06 (publicar campaña) y HU-07 (editar campaña activa)
-- Ejecutar en Supabase (SQL Editor) ANTES de levantar el backend: ddl-auto=validate.
-- Es idempotente: se puede ejecutar más de una vez sin romper nada.

-- 1) Categorías
CREATE TABLE IF NOT EXISTS categories (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO categories (name) VALUES
    ('Tecnología'),
    ('Agricultura'),
    ('Arte y cultura'),
    ('Educación'),
    ('Medio ambiente'),
    ('Comunidad'),
    ('Salud')
ON CONFLICT (name) DO NOTHING;

-- 2) Nuevas columnas de campaigns (las filas existentes reciben valores por defecto)
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS description TEXT NOT NULL DEFAULT '';
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS deadline    TIMESTAMPTZ NOT NULL DEFAULT (now() + interval '30 days');
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS category_id UUID REFERENCES categories (id);
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS created_at  TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMPTZ;

UPDATE campaigns
   SET category_id = (SELECT id FROM categories WHERE name = 'Comunidad')
 WHERE category_id IS NULL;

ALTER TABLE campaigns ALTER COLUMN category_id SET NOT NULL;

-- 3) Imágenes de la campaña (List<String> en la entidad)
CREATE TABLE IF NOT EXISTS campaign_images (
    campaign_id UUID         NOT NULL REFERENCES campaigns (id) ON DELETE CASCADE,
    image_order INT          NOT NULL,
    url         TEXT         NOT NULL,
    PRIMARY KEY (campaign_id, image_order)
);

-- 3b) Recompensas (opcionales, máx. 6 por campaña; no se editan una vez publicada la campaña)
CREATE TABLE IF NOT EXISTS rewards (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID           NOT NULL REFERENCES campaigns (id) ON DELETE CASCADE,
    name        VARCHAR(150)   NOT NULL,
    description VARCHAR(500),
    min_amount  NUMERIC(12, 2) NOT NULL CHECK (min_amount > 0),
    stock       INT            NOT NULL CHECK (stock > 0)
);

CREATE INDEX IF NOT EXISTS idx_rewards_campaign ON rewards (campaign_id);

-- 4) Historial de estados (trazabilidad; HU-08 lo reutilizará)
CREATE TABLE IF NOT EXISTS campaign_status_history (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id        UUID        NOT NULL REFERENCES campaigns (id) ON DELETE CASCADE,
    from_status        VARCHAR(20),
    to_status          VARCHAR(20) NOT NULL,
    changed_by_user_id UUID        NOT NULL,
    changed_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_campaigns_status ON campaigns (status);
CREATE INDEX IF NOT EXISTS idx_campaign_status_history_campaign ON campaign_status_history (campaign_id);
