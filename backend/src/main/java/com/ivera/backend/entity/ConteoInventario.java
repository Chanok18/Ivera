package com.ivera.backend.entity;

import com.ivera.backend.enums.ConteoEstado;
import com.ivera.backend.enums.UbicacionTipo;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conteo_inventario")
public class ConteoInventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ubicacion_tipo", nullable = false)
    private UbicacionTipo ubicacionTipo;

    @Column(name = "ubicacion_id", nullable = false)
    private Long ubicacionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConteoEstado estado = ConteoEstado.EN_PROCESO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UbicacionTipo getUbicacionTipo() { return ubicacionTipo; }
    public void setUbicacionTipo(UbicacionTipo ubicacionTipo) { this.ubicacionTipo = ubicacionTipo; }
    public Long getUbicacionId() { return ubicacionId; }
    public void setUbicacionId(Long ubicacionId) { this.ubicacionId = ubicacionId; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public ConteoEstado getEstado() { return estado; }
    public void setEstado(ConteoEstado estado) { this.estado = estado; }
}
