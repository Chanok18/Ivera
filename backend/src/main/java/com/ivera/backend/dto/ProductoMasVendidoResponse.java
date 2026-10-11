package com.ivera.backend.dto;

import java.math.BigDecimal;

public class ProductoMasVendidoResponse {
    private Long productoId;
    private String productoNombre;
    private BigDecimal cantidadTotalVendida;

    public ProductoMasVendidoResponse(Long productoId, String productoNombre, BigDecimal cantidadTotalVendida) {
        this.productoId = productoId;
        this.productoNombre = productoNombre;
        this.cantidadTotalVendida = cantidadTotalVendida;
    }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }
    public BigDecimal getCantidadTotalVendida() { return cantidadTotalVendida; }
    public void setCantidadTotalVendida(BigDecimal cantidadTotalVendida) { this.cantidadTotalVendida = cantidadTotalVendida; }
}
