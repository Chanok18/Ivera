package com.ivera.backend.controller;

import com.ivera.backend.dto.AutorizacionAlmacenRequest;
import com.ivera.backend.dto.AutorizacionAlmacenResponse;
import com.ivera.backend.service.AutorizacionAlmacenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/autorizaciones-almacen")
public class AutorizacionAlmacenController {
    private final AutorizacionAlmacenService autorizacionService;

    public AutorizacionAlmacenController(AutorizacionAlmacenService autorizacionService) {
        this.autorizacionService = autorizacionService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<AutorizacionAlmacenResponse>> listar(@RequestParam(required = false) Long trabajadorId) {
        return ResponseEntity.ok(autorizacionService.listar(trabajadorId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AutorizacionAlmacenResponse> otorgar(@Valid @RequestBody AutorizacionAlmacenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(autorizacionService.otorgar(request));
    }

    @PutMapping("/{id}/revocar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AutorizacionAlmacenResponse> revocar(@PathVariable Long id) {
        return ResponseEntity.ok(autorizacionService.revocar(id));
    }
}
