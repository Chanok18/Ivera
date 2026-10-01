package com.ivera.backend.controller;

import com.ivera.backend.dto.MovimientoRequest;
import com.ivera.backend.dto.MovimientoResponse;
import com.ivera.backend.service.MovimientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movimientos")
@Tag(name = "Movimientos", description = "Registro y gestión de movimientos de inventario")
@SecurityRequirement(name = "bearerAuth")
public class MovimientoController {

    private final MovimientoService movimientoService;

    public MovimientoController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    @PostMapping
    @Operation(summary = "Registrar un movimiento de inventario (Entrada, Salida, Ajuste, Traslado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<MovimientoResponse> registrar(@Valid @RequestBody MovimientoRequest request) {
        MovimientoResponse response = movimientoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
