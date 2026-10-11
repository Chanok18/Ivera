package com.ivera.backend.entity;

import com.ivera.backend.enums.AccionAlmacen;
import com.ivera.backend.enums.EstadoAutorizacionAlmacen;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "autorizacion_almacen_trabajador")
public class AutorizacionAlmacenTrabajador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario trabajador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccionAlmacen accion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAutorizacionAlmacen estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "otorgado_por_usuario_id", nullable = false)
    private Usuario otorgadoPor;

    @Column(name = "otorgado_en", nullable = false)
    private LocalDateTime otorgadoEn = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revocado_por_usuario_id")
    private Usuario revocadoPor;

    @Column(name = "revocado_en")
    private LocalDateTime revocadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getTrabajador() { return trabajador; }
    public void setTrabajador(Usuario trabajador) { this.trabajador = trabajador; }
    public Almacen getAlmacen() { return almacen; }
    public void setAlmacen(Almacen almacen) { this.almacen = almacen; }
    public AccionAlmacen getAccion() { return accion; }
    public void setAccion(AccionAlmacen accion) { this.accion = accion; }
    public EstadoAutorizacionAlmacen getEstado() { return estado; }
    public void setEstado(EstadoAutorizacionAlmacen estado) { this.estado = estado; }
    public Usuario getOtorgadoPor() { return otorgadoPor; }
    public void setOtorgadoPor(Usuario otorgadoPor) { this.otorgadoPor = otorgadoPor; }
    public LocalDateTime getOtorgadoEn() { return otorgadoEn; }
    public void setOtorgadoEn(LocalDateTime otorgadoEn) { this.otorgadoEn = otorgadoEn; }
    public Usuario getRevocadoPor() { return revocadoPor; }
    public void setRevocadoPor(Usuario revocadoPor) { this.revocadoPor = revocadoPor; }
    public LocalDateTime getRevocadoEn() { return revocadoEn; }
    public void setRevocadoEn(LocalDateTime revocadoEn) { this.revocadoEn = revocadoEn; }
}
