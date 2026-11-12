-- V8: Preferencias de interfaz del usuario (HU-22 tamaño de texto, HU-23 tema e idioma)

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS font_size VARCHAR(10) NOT NULL DEFAULT 'medium';

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS theme VARCHAR(10) NOT NULL DEFAULT 'light';

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS language VARCHAR(5) NOT NULL DEFAULT 'es';

COMMENT ON COLUMN users.font_size IS 'Tamaño de texto: small, medium o large';
COMMENT ON COLUMN users.theme IS 'Tema visual: light o dark';
COMMENT ON COLUMN users.language IS 'Idioma de la interfaz: es o en';
