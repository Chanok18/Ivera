-- Ejecutar manualmente en PostgreSQL tras aprobación; no se ejecuta automáticamente por la aplicación.
CREATE TABLE IF NOT EXISTS autorizacion_almacen_trabajador (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    almacen_id BIGINT NOT NULL REFERENCES almacen(id),
    accion VARCHAR(30) NOT NULL,
    estado VARCHAR(15) NOT NULL,
    otorgado_por_usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    otorgado_en TIMESTAMP NOT NULL,
    revocado_por_usuario_id BIGINT REFERENCES usuario(id),
    revocado_en TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_autorizacion_almacen_activa
    ON autorizacion_almacen_trabajador (usuario_id, almacen_id, accion)
    WHERE estado = 'ACTIVA';
