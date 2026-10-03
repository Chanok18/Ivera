package com.ivera.backend.controller;

import com.ivera.backend.dto.VentaRequest;
import com.ivera.backend.dto.VentaResponse;
import com.ivera.backend.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@Tag(name = "Ventas", description = "Registro y gestión de ventas internas")
@SecurityRequirement(name = "bearerAuth")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @PostMapping
    @Operation(summary = "Registrar una nueva venta (genera salidas, descuenta stock con validación todo o nada)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest request) {
        VentaResponse response = ventaService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar ventas (con filtro opcional ?tiendaId=)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<List<VentaResponse>> listar(@RequestParam(required = false) Long tiendaId) {
        return ResponseEntity.ok(ventaService.listar(tiendaId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener venta por ID con sus detalles")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<VentaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerPorId(id));
    }
}
