package com.ivera.backend.controller;

import com.ivera.backend.dto.UnidadResponse;
import com.ivera.backend.service.UnidadMedidaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/unidades-medida")
@Tag(name = "Unidades de medida", description = "Unidades disponibles para productos y presentaciones")
@SecurityRequirement(name = "bearerAuth")
public class UnidadMedidaController {

    private final UnidadMedidaService unidadMedidaService;

    public UnidadMedidaController(UnidadMedidaService unidadMedidaService) {
        this.unidadMedidaService = unidadMedidaService;
    }

    @GetMapping
    @Operation(summary = "Listar unidades de medida")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<List<UnidadResponse>> listar() {
        return ResponseEntity.ok(unidadMedidaService.listar());
    }
}
