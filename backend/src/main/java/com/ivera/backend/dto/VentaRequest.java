package com.ivera.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class VentaRequest {
    @NotNull(message = "El ID de la tienda es obligatorio")
    private Long tiendaId;

    @NotEmpty(message = "La venta debe tener al menos un ítem")
    private List<VentaItemRequest> items;

    public Long getTiendaId() { return tiendaId; }
    public void setTiendaId(Long tiendaId) { this.tiendaId = tiendaId; }
    public List<VentaItemRequest> getItems() { return items; }
    public void setItems(List<VentaItemRequest> items) { this.items = items; }
}
