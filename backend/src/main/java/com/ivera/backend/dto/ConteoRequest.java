package com.ivera.backend.dto;

import com.ivera.backend.enums.UbicacionTipo;
import jakarta.validation.constraints.NotNull;

public class ConteoRequest {

    @NotNull(message = "El tipo de ubicación es obligatorio")
    private UbicacionTipo ubicacionTipo;

    @NotNull(message = "El ID de la ubicación es obligatorio")
    private Long ubicacionId;

    public UbicacionTipo getUbicacionTipo() { return ubicacionTipo; }
    public void setUbicacionTipo(UbicacionTipo ubicacionTipo) { this.ubicacionTipo = ubicacionTipo; }
    public Long getUbicacionId() { return ubicacionId; }
    public void setUbicacionId(Long ubicacionId) { this.ubicacionId = ubicacionId; }
}
