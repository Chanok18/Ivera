package com.ivera.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class VentaResponse {
    private Long id;
    private Long tiendaId;
    private String tiendaNombre;
    private Long usuarioId;
    private String usuarioNombre;
    private LocalDateTime fecha;
    private BigDecimal total;
    private List<VentaDetalleResponse> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public List<VentaDetalleResponse> getItems() { return items; }
    public void setItems(List<VentaDetalleResponse> items) { this.items = items; }
}
