package com.ivera.backend.repository;

import com.ivera.backend.entity.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {
    Optional<UnidadMedida> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
}