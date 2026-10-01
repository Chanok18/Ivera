package com.ivera.backend.dto;

import java.math.BigDecimal;

public class PresentacionResponse {

    private Long id;
    private UnidadResponse unidad;
    private Integer factorConversion;
    private BigDecimal precioPresentacion;
    private String equivalencia;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UnidadResponse getUnidad() { return unidad; }
    public void setUnidad(UnidadResponse unidad) { this.unidad = unidad; }
    public Integer getFactorConversion() { return factorConversion; }
    public void setFactorConversion(Integer factorConversion) { this.factorConversion = factorConversion; }
    public BigDecimal getPrecioPresentacion() { return precioPresentacion; }
    public void setPrecioPresentacion(BigDecimal precioPresentacion) { this.precioPresentacion = precioPresentacion; }
    public String getEquivalencia() { return equivalencia; }
    public void setEquivalencia(String equivalencia) { this.equivalencia = equivalencia; }
}