package com.ivera.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class ProveedorRequest {
    @NotBlank(message = "El nombre del proveedor es obligatorio")
    private String nombre;

    @NotBlank(message = "El RUC del proveedor es obligatorio")
    private String ruc;

    private String contacto;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }
}
