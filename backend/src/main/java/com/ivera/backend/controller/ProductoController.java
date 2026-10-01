package com.ivera.backend.controller;

import com.ivera.backend.dto.ProductoRequest;
import com.ivera.backend.dto.ProductoResponse;
import com.ivera.backend.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@SecurityRequirement(name = "bearer-jwt")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Listar productos", description = "Busca por nombre si se envía ?nombre=, si no lista todos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<List<ProductoResponse>> listar(
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(productoService.listar(nombre));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @GetMapping("/buscar/codigo")
    @Operation(summary = "Buscar producto por código de barras")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<ProductoResponse> buscarPorCodigo(@RequestParam String codigo) {
        return ResponseEntity.ok(productoService.buscarPorCodigoBarras(codigo));
    }

    @PostMapping
    @Operation(summary = "Crear producto (solo ADMINISTRADOR)")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto (solo ADMINISTRADOR)")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto (solo ADMINISTRADOR)")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}