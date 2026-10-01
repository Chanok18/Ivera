package com.ivera.backend.repository;

import com.ivera.backend.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByNombreContainingIgnoreCase(String nombre);
    Optional<Producto> findByCodigoBarras(String codigoBarras);
}