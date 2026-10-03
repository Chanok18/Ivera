package com.ivera.backend.dto;

public class ProveedorResponse {
    private Long id;
    private String nombre;
    private String ruc;
    private String contacto;

    public ProveedorResponse(Long id, String nombre, String ruc, String contacto) {
        this.id = id;
        this.nombre = nombre;
        this.ruc = ruc;
        this.contacto = contacto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }
}
