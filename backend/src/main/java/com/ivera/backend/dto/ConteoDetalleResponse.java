package com.ivera.backend.dto;

import java.math.BigDecimal;

public class ConteoDetalleResponse {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private BigDecimal cantidadContada;
    private BigDecimal cantidadSistema;
    private BigDecimal diferencia;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }
    public BigDecimal getCantidadContada() { return cantidadContada; }
    public void setCantidadContada(BigDecimal cantidadContada) { this.cantidadContada = cantidadContada; }
    public BigDecimal getCantidadSistema() { return cantidadSistema; }
    public void setCantidadSistema(BigDecimal cantidadSistema) { this.cantidadSistema = cantidadSistema; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }
}
