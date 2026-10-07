-- No se pueden deducir DNI de correos existentes. Se conservan esas cuentas
-- con DNI NULL hasta que un operador asigne un documento verificado.
ALTER TABLE users ADD COLUMN dni VARCHAR(8);
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_dni_format_ck
    CHECK (dni IS NULL OR (dni ~ '^[0-9]{7,8}$' AND dni !~ '^0+$'));
CREATE UNIQUE INDEX users_dni_uq ON users (dni) WHERE dni IS NOT NULL;
-- Nuevos registros deben tener documento; filas heredadas conservan su correo.
ALTER TABLE users ADD CONSTRAINT users_identity_present_ck
    CHECK (dni IS NOT NULL OR email IS NOT NULL);
