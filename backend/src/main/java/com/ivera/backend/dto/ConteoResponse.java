package com.ivera.backend.dto;

import com.ivera.backend.enums.ConteoEstado;
import com.ivera.backend.enums.UbicacionTipo;
import java.time.LocalDateTime;
import java.util.List;

public class ConteoResponse {
    private Long id;
    private UbicacionTipo ubicacionTipo;
    private Long ubicacionId;
    private String ubicacionNombre;
    private Long usuarioId;
    private String usuarioNombre;
    private LocalDateTime fecha;
    private ConteoEstado estado;
    private List<ConteoDetalleResponse> detalles;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UbicacionTipo getUbicacionTipo() { return ubicacionTipo; }
    public void setUbicacionTipo(UbicacionTipo ubicacionTipo) { this.ubicacionTipo = ubicacionTipo; }
    public Long getUbicacionId() { return ubicacionId; }
    public void setUbicacionId(Long ubicacionId) { this.ubicacionId = ubicacionId; }
    public String getUbicacionNombre() { return ubicacionNombre; }
    public void setUbicacionNombre(String ubicacionNombre) { this.ubicacionNombre = ubicacionNombre; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public ConteoEstado getEstado() { return estado; }
    public void setEstado(ConteoEstado estado) { this.estado = estado; }
    public List<ConteoDetalleResponse> getDetalles() { return detalles; }
    public void setDetalles(List<ConteoDetalleResponse> detalles) { this.detalles = detalles; }
}
