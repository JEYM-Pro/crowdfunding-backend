-- V6: Las imágenes subidas se guardan como data URLs, que superan VARCHAR(500).
-- Ejecutar en Supabase (SQL Editor); es seguro repetirlo.

ALTER TABLE campaign_images
    ALTER COLUMN url TYPE TEXT;
