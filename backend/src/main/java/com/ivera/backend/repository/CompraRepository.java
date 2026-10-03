package com.ivera.backend.repository;

import com.ivera.backend.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByTiendaId(Long tiendaId);
}
