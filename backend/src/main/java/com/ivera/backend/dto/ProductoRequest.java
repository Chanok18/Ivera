package com.ivera.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public class ProductoRequest {

    @NotBlank
    private String nombre;

    private String descripcion;

    private String categoria;

    private String codigoBarras;

    @NotNull
    private Long unidadBaseId;

    private String imagenUrl;

    @NotNull
    @Positive
    private BigDecimal precioUnitario;

    private List<PresentacionRequest> presentaciones;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
    public Long getUnidadBaseId() { return unidadBaseId; }
    public void setUnidadBaseId(Long unidadBaseId) { this.unidadBaseId = unidadBaseId; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public List<PresentacionRequest> getPresentaciones() { return presentaciones; }
    public void setPresentaciones(List<PresentacionRequest> presentaciones) { this.presentaciones = presentaciones; }
}