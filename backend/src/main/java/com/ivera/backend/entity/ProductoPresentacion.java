package com.ivera.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class ProductoPresentacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_id", nullable = false)
    private UnidadMedida unidad;

    @Column(nullable = false)
    private Integer factorConversion;

    @Column(precision = 10, scale = 2)
    private BigDecimal precioPresentacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public UnidadMedida getUnidad() { return unidad; }
    public void setUnidad(UnidadMedida unidad) { this.unidad = unidad; }
    public Integer getFactorConversion() { return factorConversion; }
    public void setFactorConversion(Integer factorConversion) { this.factorConversion = factorConversion; }
    public BigDecimal getPrecioPresentacion() { return precioPresentacion; }
    public void setPrecioPresentacion(BigDecimal precioPresentacion) { this.precioPresentacion = precioPresentacion; }
}