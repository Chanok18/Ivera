package com.ivera.backend.service;

import com.ivera.backend.dto.UnidadResponse;
import com.ivera.backend.repository.UnidadMedidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UnidadMedidaService {

    private final UnidadMedidaRepository unidadMedidaRepository;

    public UnidadMedidaService(UnidadMedidaRepository unidadMedidaRepository) {
        this.unidadMedidaRepository = unidadMedidaRepository;
    }

    @Transactional(readOnly = true)
    public List<UnidadResponse> listar() {
        return unidadMedidaRepository.findAll().stream()
                .map(unidad -> new UnidadResponse(unidad.getId(), unidad.getNombre()))
                .toList();
    }
}
