package com.ivera.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ConteoDetalleRequest {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @NotNull(message = "La cantidad contada es obligatoria")
    @DecimalMin(value = "0.0", message = "La cantidad contada no puede ser negativa")
    private BigDecimal cantidadContada;

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public BigDecimal getCantidadContada() { return cantidadContada; }
    public void setCantidadContada(BigDecimal cantidadContada) { this.cantidadContada = cantidadContada; }
}
