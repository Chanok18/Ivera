package com.ivera.backend.service;

import com.ivera.backend.dto.ProveedorRequest;
import com.ivera.backend.dto.ProveedorResponse;
import com.ivera.backend.entity.Proveedor;
import com.ivera.backend.repository.ProveedorRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return proveedorRepository.findAll().stream()
                .map(p -> new ProveedorResponse(p.getId(), p.getNombre(), p.getRuc(), p.getContacto()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtenerPorId(Long id) {
        Proveedor p = proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + id));
        return new ProveedorResponse(p.getId(), p.getNombre(), p.getRuc(), p.getContacto());
    }

    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        Proveedor p = new Proveedor();
        p.setNombre(request.getNombre());
        p.setRuc(request.getRuc());
        p.setContacto(request.getContacto());
        p = proveedorRepository.save(p);
        return new ProveedorResponse(p.getId(), p.getNombre(), p.getRuc(), p.getContacto());
    }

    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        Proveedor p = proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + id));
        p.setNombre(request.getNombre());
        p.setRuc(request.getRuc());
        p.setContacto(request.getContacto());
        p = proveedorRepository.save(p);
        return new ProveedorResponse(p.getId(), p.getNombre(), p.getRuc(), p.getContacto());
    }

    @Transactional
    public void eliminar(Long id) {
        Proveedor p = proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + id));
        proveedorRepository.delete(p);
    }
}
