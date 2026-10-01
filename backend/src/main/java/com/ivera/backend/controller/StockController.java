package com.ivera.backend.controller;

import com.ivera.backend.dto.StockResponse;
import com.ivera.backend.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@Tag(name = "Stock", description = "Gestión de inventario y stock consolidado")
@SecurityRequirement(name = "bearerAuth")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{productoId}")
    @Operation(summary = "Obtener stock consolidado de un producto por ubicación (Tiendas y Almacén)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<List<StockResponse>> obtenerStockConsolidado(@PathVariable Long productoId) {
        List<StockResponse> stock = stockService.obtenerStockConsolidado(productoId);
        return ResponseEntity.ok(stock);
    }
}
