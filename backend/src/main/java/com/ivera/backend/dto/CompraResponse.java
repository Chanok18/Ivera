package com.ivera.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CompraResponse {
    private Long id;
    private Long proveedorId;
    private String proveedorNombre;
    private Long tiendaId;
    private String tiendaNombre;
    private Long usuarioId;
    private String usuarioNombre;
    private LocalDateTime fecha;
    private BigDecimal total;
    private String estado;
    private List<CompraDetalleResponse> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProveedorId() { return proveedorId; }
    public void setProveedorId(Long proveedorId) { this.proveedorId = proveedorId; }
    public String getProveedorNombre() { return proveedorNombre; }
    public void setProveedorNombre(String proveedorNombre) { this.proveedorNombre = proveedorNombre; }
    public Long getTiendaId() { return tiendaId; }
    public void setTiendaId(Long tiendaId) { this.tiendaId = tiendaId; }
    public String getTiendaNombre() { return tiendaNombre; }
    public void setTiendaNombre(String tiendaNombre) { this.tiendaNombre = tiendaNombre; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<CompraDetalleResponse> getItems() { return items; }
    public void setItems(List<CompraDetalleResponse> items) { this.items = items; }
}
