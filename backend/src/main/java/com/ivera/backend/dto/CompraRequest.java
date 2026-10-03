package com.ivera.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CompraRequest {
    @NotNull(message = "El ID del proveedor es obligatorio")
    private Long proveedorId;

    @NotNull(message = "El ID de la tienda es obligatorio")
    private Long tiendaId;

    @NotEmpty(message = "La compra debe tener al menos un ítem")
    private List<CompraItemRequest> items;

    public Long getProveedorId() { return proveedorId; }
    public void setProveedorId(Long proveedorId) { this.proveedorId = proveedorId; }
    public Long getTiendaId() { return tiendaId; }
    public void setTiendaId(Long tiendaId) { this.tiendaId = tiendaId; }
    public List<CompraItemRequest> getItems() { return items; }
    public void setItems(List<CompraItemRequest> items) { this.items = items; }
}
