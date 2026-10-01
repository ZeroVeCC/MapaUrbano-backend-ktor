-- Catálogo de docs/02-modulos-backend.md. No reemplaza personalizaciones existentes.
INSERT INTO categories (slug, name, color_hex, sort_order) VALUES
    ('bache', 'Bache', '#EA580C', 10),
    ('luminaria', 'Luminaria', '#2563EB', 20),
    ('basura', 'Basura', '#6B7280', 30),
    ('vandalismo', 'Vandalismo', '#7C3AED', 40),
    ('inundacion', 'Inundación', '#1E3A8A', 50),
    ('otro', 'Otro', '#0F766E', 60)
ON CONFLICT (slug) DO NOTHING;
