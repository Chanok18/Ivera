package com.ivera.backend.dto;

import com.ivera.backend.enums.MovimientoTipo;
import com.ivera.backend.enums.UbicacionTipo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class MovimientoRequest {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private MovimientoTipo tipo;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero")
    private BigDecimal cantidad;

    @NotNull(message = "El ID de la unidad es obligatorio")
    private Long unidadId;

    @NotNull(message = "El tipo de ubicación de origen es obligatorio")
    private UbicacionTipo ubicacionOrigenTipo;

    @NotNull(message = "El ID de la ubicación de origen es obligatorio")
    private Long ubicacionOrigenId;

    private UbicacionTipo ubicacionDestinoTipo;
    private Long ubicacionDestinoId;

    private String motivo;

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public MovimientoTipo getTipo() { return tipo; }
    public void setTipo(MovimientoTipo tipo) { this.tipo = tipo; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public Long getUnidadId() { return unidadId; }
    public void setUnidadId(Long unidadId) { this.unidadId = unidadId; }
    public UbicacionTipo getUbicacionOrigenTipo() { return ubicacionOrigenTipo; }
    public void setUbicacionOrigenTipo(UbicacionTipo ubicacionOrigenTipo) { this.ubicacionOrigenTipo = ubicacionOrigenTipo; }
    public Long getUbicacionOrigenId() { return ubicacionOrigenId; }
    public void setUbicacionOrigenId(Long ubicacionOrigenId) { this.ubicacionOrigenId = ubicacionOrigenId; }
    public UbicacionTipo getUbicacionDestinoTipo() { return ubicacionDestinoTipo; }
    public void setUbicacionDestinoTipo(UbicacionTipo ubicacionDestinoTipo) { this.ubicacionDestinoTipo = ubicacionDestinoTipo; }
    public Long getUbicacionDestinoId() { return ubicacionDestinoId; }
    public void setUbicacionDestinoId(Long ubicacionDestinoId) { this.ubicacionDestinoId = ubicacionDestinoId; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
