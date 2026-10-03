package com.ivera.backend.controller;

import com.ivera.backend.dto.ConteoDetalleRequest;
import com.ivera.backend.dto.ConteoRequest;
import com.ivera.backend.dto.ConteoResponse;
import com.ivera.backend.service.ConteoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conteos")
@Tag(name = "Conteo de Inventario", description = "Gestión de conteos físicos y diferencias")
@SecurityRequirement(name = "bearerAuth")
public class ConteoController {

    private final ConteoService conteoService;

    public ConteoController(ConteoService conteoService) {
        this.conteoService = conteoService;
    }

    @PostMapping
    @Operation(summary = "Iniciar un nuevo conteo de inventario para una ubicación")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<ConteoResponse> iniciar(@Valid @RequestBody ConteoRequest request) {
        ConteoResponse response = conteoService.iniciar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/detalle")
    @Operation(summary = "Agregar o actualizar el conteo de un producto en un conteo activo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<ConteoResponse> agregarDetalle(
            @PathVariable Long id,
            @Valid @RequestBody ConteoDetalleRequest request) {
        ConteoResponse response = conteoService.agregarDetalle(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/finalizar")
    @Operation(summary = "Finalizar un conteo de inventario")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<ConteoResponse> finalizar(@PathVariable Long id) {
        ConteoResponse response = conteoService.finalizar(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un conteo de inventario con sus detalles y diferencias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<ConteoResponse> obtenerPorId(@PathVariable Long id) {
        ConteoResponse response = conteoService.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }
}
