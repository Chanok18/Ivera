package com.ivera.backend.dto;

import com.ivera.backend.enums.UbicacionTipo;
import java.math.BigDecimal;

public class StockResponse {
    private Long ubicacionId;
    private UbicacionTipo ubicacionTipo;
    private String ubicacionNombre;
    private BigDecimal cantidad;

    public StockResponse(Long ubicacionId, UbicacionTipo ubicacionTipo, String ubicacionNombre, BigDecimal cantidad) {
        this.ubicacionId = ubicacionId;
        this.ubicacionTipo = ubicacionTipo;
        this.ubicacionNombre = ubicacionNombre;
        this.cantidad = cantidad;
    }

    public Long getUbicacionId() { return ubicacionId; }
    public void setUbicacionId(Long ubicacionId) { this.ubicacionId = ubicacionId; }
    public UbicacionTipo getUbicacionTipo() { return ubicacionTipo; }
    public void setUbicacionTipo(UbicacionTipo ubicacionTipo) { this.ubicacionTipo = ubicacionTipo; }
    public String getUbicacionNombre() { return ubicacionNombre; }
    public void setUbicacionNombre(String ubicacionNombre) { this.ubicacionNombre = ubicacionNombre; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
}
