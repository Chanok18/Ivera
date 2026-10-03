package com.ivera.backend.repository;

import com.ivera.backend.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByTiendaId(Long tiendaId);
}
