package com.ivera.backend.repository;

import com.ivera.backend.entity.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
    boolean existsByNombre(String nombre);
}