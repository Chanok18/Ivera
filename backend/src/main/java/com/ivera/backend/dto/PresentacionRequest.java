package com.ivera.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class PresentacionRequest {

    @NotNull
    private Long unidadId;

    @NotNull
    @Positive
    private Integer factorConversion;

    private BigDecimal precioPresentacion;

    public Long getUnidadId() { return unidadId; }
    public void setUnidadId(Long unidadId) { this.unidadId = unidadId; }
    public Integer getFactorConversion() { return factorConversion; }
    public void setFactorConversion(Integer factorConversion) { this.factorConversion = factorConversion; }
    public BigDecimal getPrecioPresentacion() { return precioPresentacion; }
    public void setPrecioPresentacion(BigDecimal precioPresentacion) { this.precioPresentacion = precioPresentacion; }
}