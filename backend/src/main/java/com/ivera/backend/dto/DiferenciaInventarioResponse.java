package com.ivera.backend.dto;

import com.ivera.backend.enums.UbicacionTipo;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DiferenciaInventarioResponse {
    private Long conteoId;
    private Long productoId;
    private String productoNombre;
    private UbicacionTipo ubicacionTipo;
    private Long ubicacionId;
    private String ubicacionNombre;
    private BigDecimal cantidadContada;
    private BigDecimal cantidadSistema;
    private BigDecimal diferencia;
    private LocalDateTime fecha;

    public Long getConteoId() { return conteoId; }
    public void setConteoId(Long conteoId) { this.conteoId = conteoId; }
    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }
    public UbicacionTipo getUbicacionTipo() { return ubicacionTipo; }
    public void setUbicacionTipo(UbicacionTipo ubicacionTipo) { this.ubicacionTipo = ubicacionTipo; }
    public Long getUbicacionId() { return ubicacionId; }
    public void setUbicacionId(Long ubicacionId) { this.ubicacionId = ubicacionId; }
    public String getUbicacionNombre() { return ubicacionNombre; }
    public void setUbicacionNombre(String ubicacionNombre) { this.ubicacionNombre = ubicacionNombre; }
    public BigDecimal getCantidadContada() { return cantidadContada; }
    public void setCantidadContada(BigDecimal cantidadContada) { this.cantidadContada = cantidadContada; }
    public BigDecimal getCantidadSistema() { return cantidadSistema; }
    public void setCantidadSistema(BigDecimal cantidadSistema) { this.cantidadSistema = cantidadSistema; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
