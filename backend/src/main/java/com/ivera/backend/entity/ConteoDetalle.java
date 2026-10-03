package com.ivera.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "conteo_detalle")
public class ConteoDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conteo_id", nullable = false)
    private ConteoInventario conteo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad_contada", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidadContada;

    @Column(name = "cantidad_sistema", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidadSistema;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal diferencia; // cantidad_contada - cantidad_sistema

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ConteoInventario getConteo() { return conteo; }
    public void setConteo(ConteoInventario conteo) { this.conteo = conteo; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public BigDecimal getCantidadContada() { return cantidadContada; }
    public void setCantidadContada(BigDecimal cantidadContada) { this.cantidadContada = cantidadContada; }
    public BigDecimal getCantidadSistema() { return cantidadSistema; }
    public void setCantidadSistema(BigDecimal cantidadSistema) { this.cantidadSistema = cantidadSistema; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }
}
