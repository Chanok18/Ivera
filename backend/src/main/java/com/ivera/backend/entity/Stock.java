package com.ivera.backend.entity;

import com.ivera.backend.enums.UbicacionTipo;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "stock", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"producto_id", "ubicacion_tipo", "ubicacion_id"})
})
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(name = "ubicacion_tipo", nullable = false)
    private UbicacionTipo ubicacionTipo;

    @Column(name = "ubicacion_id", nullable = false)
    private Long ubicacionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public UbicacionTipo getUbicacionTipo() { return ubicacionTipo; }
    public void setUbicacionTipo(UbicacionTipo ubicacionTipo) { this.ubicacionTipo = ubicacionTipo; }
    public Long getUbicacionId() { return ubicacionId; }
    public void setUbicacionId(Long ubicacionId) { this.ubicacionId = ubicacionId; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
}
