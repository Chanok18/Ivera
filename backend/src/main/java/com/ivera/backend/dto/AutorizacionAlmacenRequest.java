package com.ivera.backend.dto;

import com.ivera.backend.enums.AccionAlmacen;
import jakarta.validation.constraints.NotNull;

public class AutorizacionAlmacenRequest {
    @NotNull(message = "El ID del trabajador es obligatorio")
    private Long trabajadorId;
    @NotNull(message = "El ID del almacén es obligatorio")
    private Long almacenId;
    @NotNull(message = "La acción autorizada es obligatoria")
    private AccionAlmacen accion;

    public Long getTrabajadorId() { return trabajadorId; }
    public void setTrabajadorId(Long trabajadorId) { this.trabajadorId = trabajadorId; }
    public Long getAlmacenId() { return almacenId; }
    public void setAlmacenId(Long almacenId) { this.almacenId = almacenId; }
    public AccionAlmacen getAccion() { return accion; }
    public void setAccion(AccionAlmacen accion) { this.accion = accion; }
}
