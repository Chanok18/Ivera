-- Ejecutar manualmente en PostgreSQL tras aprobación; no se ejecuta automáticamente por la aplicación.
ALTER TABLE venta_detalle
    ADD COLUMN IF NOT EXISTS cantidad_base NUMERIC(10, 2);

COMMENT ON COLUMN venta_detalle.cantidad_base IS
    'Cantidad vendida en la unidad base, capturada al confirmar ventas nuevas.';
