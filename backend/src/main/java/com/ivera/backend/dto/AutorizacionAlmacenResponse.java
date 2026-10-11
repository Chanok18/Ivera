package com.ivera.backend.dto;

import com.ivera.backend.enums.AccionAlmacen;
import com.ivera.backend.enums.EstadoAutorizacionAlmacen;

import java.time.LocalDateTime;

public class AutorizacionAlmacenResponse {
    private Long id;
    private Long trabajadorId;
    private String trabajadorNombre;
    private Long almacenId;
    private String almacenNombre;
    private AccionAlmacen accion;
    private EstadoAutorizacionAlmacen estado;
    private Long otorgadoPorId;
    private String otorgadoPorNombre;
    private LocalDateTime otorgadoEn;
    private Long revocadoPorId;
    private String revocadoPorNombre;
    private LocalDateTime revocadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTrabajadorId() { return trabajadorId; }
    public void setTrabajadorId(Long trabajadorId) { this.trabajadorId = trabajadorId; }
    public String getTrabajadorNombre() { return trabajadorNombre; }
    public void setTrabajadorNombre(String trabajadorNombre) { this.trabajadorNombre = trabajadorNombre; }
    public Long getAlmacenId() { return almacenId; }
    public void setAlmacenId(Long almacenId) { this.almacenId = almacenId; }
    public String getAlmacenNombre() { return almacenNombre; }
    public void setAlmacenNombre(String almacenNombre) { this.almacenNombre = almacenNombre; }
    public AccionAlmacen getAccion() { return accion; }
    public void setAccion(AccionAlmacen accion) { this.accion = accion; }
    public EstadoAutorizacionAlmacen getEstado() { return estado; }
    public void setEstado(EstadoAutorizacionAlmacen estado) { this.estado = estado; }
    public Long getOtorgadoPorId() { return otorgadoPorId; }
    public void setOtorgadoPorId(Long otorgadoPorId) { this.otorgadoPorId = otorgadoPorId; }
    public String getOtorgadoPorNombre() { return otorgadoPorNombre; }
    public void setOtorgadoPorNombre(String otorgadoPorNombre) { this.otorgadoPorNombre = otorgadoPorNombre; }
    public LocalDateTime getOtorgadoEn() { return otorgadoEn; }
    public void setOtorgadoEn(LocalDateTime otorgadoEn) { this.otorgadoEn = otorgadoEn; }
    public Long getRevocadoPorId() { return revocadoPorId; }
    public void setRevocadoPorId(Long revocadoPorId) { this.revocadoPorId = revocadoPorId; }
    public String getRevocadoPorNombre() { return revocadoPorNombre; }
    public void setRevocadoPorNombre(String revocadoPorNombre) { this.revocadoPorNombre = revocadoPorNombre; }
    public LocalDateTime getRevocadoEn() { return revocadoEn; }
    public void setRevocadoEn(LocalDateTime revocadoEn) { this.revocadoEn = revocadoEn; }
}
