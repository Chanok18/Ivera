package com.ivera.backend.repository;

import com.ivera.backend.entity.Tienda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TiendaRepository extends JpaRepository<Tienda, Long> {
    boolean existsByNombre(String nombre);
}