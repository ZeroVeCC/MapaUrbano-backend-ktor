-- Solo se descubre en la prueba de rollback; nunca se empaqueta en producción.
CREATE TABLE partial_migration (id INTEGER PRIMARY KEY);
SELECT 1 / 0;
