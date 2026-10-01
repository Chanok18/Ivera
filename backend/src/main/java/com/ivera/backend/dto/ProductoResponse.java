package com.ivera.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductoResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private String categoria;
    private String codigoBarras;
    private UnidadResponse unidadBase;
    private String imagenUrl;
    private BigDecimal precioUnitario;
    private List<PresentacionResponse> presentaciones;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
    public UnidadResponse getUnidadBase() { return unidadBase; }
    public void setUnidadBase(UnidadResponse unidadBase) { this.unidadBase = unidadBase; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public List<PresentacionResponse> getPresentaciones() { return presentaciones; }
    public void setPresentaciones(List<PresentacionResponse> presentaciones) { this.presentaciones = presentaciones; }
}