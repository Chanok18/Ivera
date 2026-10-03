package com.ivera.backend.controller;

import com.ivera.backend.dto.CompraRequest;
import com.ivera.backend.dto.CompraResponse;
import com.ivera.backend.service.CompraService;
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
@RequestMapping("/api/compras")
@Tag(name = "Compras", description = "Registro y gestión de compras a proveedores")
@SecurityRequirement(name = "bearerAuth")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping
    @Operation(summary = "Registrar una nueva compra (genera entradas y suma stock automáticamente)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<CompraResponse> registrar(@Valid @RequestBody CompraRequest request) {
        CompraResponse response = compraService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar compras (con filtro opcional ?tiendaId=)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<List<CompraResponse>> listar(@RequestParam(required = false) Long tiendaId) {
        return ResponseEntity.ok(compraService.listar(tiendaId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener compra por ID con sus detalles")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<CompraResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.obtenerPorId(id));
    }
}
