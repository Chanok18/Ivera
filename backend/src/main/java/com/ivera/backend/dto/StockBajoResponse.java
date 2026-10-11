package com.ivera.backend.dto;

import java.math.BigDecimal;

public class StockBajoResponse {
    private Long productoId;
    private String productoNombre;
    private String codigoBarras;
    private BigDecimal stockTotal;

    public StockBajoResponse(Long productoId, String productoNombre, String codigoBarras, BigDecimal stockTotal) {
        this.productoId = productoId;
        this.productoNombre = productoNombre;
        this.codigoBarras = codigoBarras;
        this.stockTotal = stockTotal;
    }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }
    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
    public BigDecimal getStockTotal() { return stockTotal; }
    public void setStockTotal(BigDecimal stockTotal) { this.stockTotal = stockTotal; }
}
