package com.ivera.backend.dto;

import java.math.BigDecimal;

public class VentaDetalleResponse {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private BigDecimal cantidad;
    private UnidadResponse unidad;
    private BigDecimal precioUnitario;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public UnidadResponse getUnidad() { return unidad; }
    public void setUnidad(UnidadResponse unidad) { this.unidad = unidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
}
