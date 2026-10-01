package com.ivera.backend.dto;

import com.ivera.backend.enums.MovimientoEstado;
import com.ivera.backend.enums.MovimientoTipo;
import com.ivera.backend.enums.UbicacionTipo;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovimientoResponse {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private MovimientoTipo tipo;
    private BigDecimal cantidad;
    private UnidadResponse unidad;
    private UbicacionTipo ubicacionOrigenTipo;
    private Long ubicacionOrigenId;
    private String ubicacionOrigenNombre;
    private UbicacionTipo ubicacionDestinoTipo;
    private Long ubicacionDestinoId;
    private String ubicacionDestinoNombre;
    private Long usuarioId;
    private String usuarioNombre;
    private MovimientoEstado estado;
    private Long aprobadoPorId;
    private String aprobadoPorNombre;
    private LocalDateTime fecha;
    private String motivo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }
    public MovimientoTipo getTipo() { return tipo; }
    public void setTipo(MovimientoTipo tipo) { this.tipo = tipo; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public UnidadResponse getUnidad() { return unidad; }
    public void setUnidad(UnidadResponse unidad) { this.unidad = unidad; }
    public UbicacionTipo getUbicacionOrigenTipo() { return ubicacionOrigenTipo; }
    public void setUbicacionOrigenTipo(UbicacionTipo ubicacionOrigenTipo) { this.ubicacionOrigenTipo = ubicacionOrigenTipo; }
    public Long getUbicacionOrigenId() { return ubicacionOrigenId; }
    public void setUbicacionOrigenId(Long ubicacionOrigenId) { this.ubicacionOrigenId = ubicacionOrigenId; }
    public String getUbicacionOrigenNombre() { return ubicacionOrigenNombre; }
    public void setUbicacionOrigenNombre(String ubicacionOrigenNombre) { this.ubicacionOrigenNombre = ubicacionOrigenNombre; }
    public UbicacionTipo getUbicacionDestinoTipo() { return ubicacionDestinoTipo; }
    public void setUbicacionDestinoTipo(UbicacionTipo ubicacionDestinoTipo) { this.ubicacionDestinoTipo = ubicacionDestinoTipo; }
    public Long getUbicacionDestinoId() { return ubicacionDestinoId; }
    public void setUbicacionDestinoId(Long ubicacionDestinoId) { this.ubicacionDestinoId = ubicacionDestinoId; }
    public String getUbicacionDestinoNombre() { return ubicacionDestinoNombre; }
    public void setUbicacionDestinoNombre(String ubicacionDestinoNombre) { this.ubicacionDestinoNombre = ubicacionDestinoNombre; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public MovimientoEstado getEstado() { return estado; }
    public void setEstado(MovimientoEstado estado) { this.estado = estado; }
    public Long getAprobadoPorId() { return aprobadoPorId; }
    public void setAprobadoPorId(Long aprobadoPorId) { this.aprobadoPorId = aprobadoPorId; }
    public String getAprobadoPorNombre() { return aprobadoPorNombre; }
    public void setAprobadoPorNombre(String aprobadoPorNombre) { this.aprobadoPorNombre = aprobadoPorNombre; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
