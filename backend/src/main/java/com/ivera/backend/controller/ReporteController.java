package com.ivera.backend.controller;

import com.ivera.backend.dto.*;
import com.ivera.backend.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@Validated
@Tag(name = "Reportes", description = "Reportes de inventario, stock bajo, diferencias, movimientos y más vendidos")
@SecurityRequirement(name = "bearerAuth")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/stock-bajo")
    @Operation(summary = "Reporte de productos con stock bajo (menor o igual al umbral, default 10)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<List<StockBajoResponse>> obtenerStockBajo(
            @RequestParam(required = false) @DecimalMin(value = "0", message = "El umbral debe ser mayor o igual a 0") BigDecimal umbral) {
        return ResponseEntity.ok(reporteService.obtenerStockBajo(umbral));
    }

    @GetMapping("/diferencias-inventario")
    @Operation(summary = "Reporte de diferencias de inventario en conteos físicos por rango de fechas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<List<DiferenciaInventarioResponse>> obtenerDiferenciasInventario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.obtenerDiferenciasInventario(desde, hasta));
    }

    @GetMapping("/movimientos")
    @Operation(summary = "Reporte de movimientos filtrados por tienda y rango de fechas (ordenados del más reciente)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<List<MovimientoResponse>> obtenerMovimientos(
            @RequestParam(required = false) @Positive(message = "tiendaId debe ser un número positivo") Long tiendaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.obtenerMovimientos(tiendaId, desde, hasta));
    }

    @GetMapping("/productos-mas-vendidos")
    @Operation(summary = "Reporte de productos más vendidos (top N, default 5)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<List<ProductoMasVendidoResponse>> obtenerProductosMasVendidos(
            @RequestParam(required = false) @Positive(message = "El límite debe ser mayor que 0") Integer limite) {
        return ResponseEntity.ok(reporteService.obtenerProductosMasVendidos(limite));
    }
}
