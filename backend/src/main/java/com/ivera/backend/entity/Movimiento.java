package com.ivera.backend.entity;

import com.ivera.backend.enums.MovimientoEstado;
import com.ivera.backend.enums.MovimientoTipo;
import com.ivera.backend.enums.UbicacionTipo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento")
public class Movimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovimientoTipo tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad; // en unidad base

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_id", nullable = false)
    private UnidadMedida unidad; // unidad original en que se registró

    @Enumerated(EnumType.STRING)
    @Column(name = "ubicacion_origen_tipo", nullable = false)
    private UbicacionTipo ubicacionOrigenTipo;

    @Column(name = "ubicacion_origen_id", nullable = false)
    private Long ubicacionOrigenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ubicacion_destino_tipo")
    private UbicacionTipo ubicacionDestinoTipo;

    @Column(name = "ubicacion_destino_id")
    private Long ubicacionDestinoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // quien lo registró

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovimientoEstado estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    private Usuario aprobadoPor; // nullable

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String motivo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public MovimientoTipo getTipo() { return tipo; }
    public void setTipo(MovimientoTipo tipo) { this.tipo = tipo; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public UnidadMedida getUnidad() { return unidad; }
    public void setUnidad(UnidadMedida unidad) { this.unidad = unidad; }
    public UbicacionTipo getUbicacionOrigenTipo() { return ubicacionOrigenTipo; }
    public void setUbicacionOrigenTipo(UbicacionTipo ubicacionOrigenTipo) { this.ubicacionOrigenTipo = ubicacionOrigenTipo; }
    public Long getUbicacionOrigenId() { return ubicacionOrigenId; }
    public void setUbicacionOrigenId(Long ubicacionOrigenId) { this.ubicacionOrigenId = ubicacionOrigenId; }
    public UbicacionTipo getUbicacionDestinoTipo() { return ubicacionDestinoTipo; }
    public void setUbicacionDestinoTipo(UbicacionTipo ubicacionDestinoTipo) { this.ubicacionDestinoTipo = ubicacionDestinoTipo; }
    public Long getUbicacionDestinoId() { return ubicacionDestinoId; }
    public void setUbicacionDestinoId(Long ubicacionDestinoId) { this.ubicacionDestinoId = ubicacionDestinoId; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public MovimientoEstado getEstado() { return estado; }
    public void setEstado(MovimientoEstado estado) { this.estado = estado; }
    public Usuario getAprobadoPor() { return aprobadoPor; }
    public void setAprobadoPor(Usuario aprobadoPor) { this.aprobadoPor = aprobadoPor; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
