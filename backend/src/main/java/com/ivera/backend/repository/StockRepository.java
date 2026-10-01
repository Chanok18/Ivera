package com.ivera.backend.repository;

import com.ivera.backend.entity.Stock;
import com.ivera.backend.enums.UbicacionTipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByProductoId(Long productoId);
    Optional<Stock> findByProductoIdAndUbicacionTipoAndUbicacionId(Long productoId, UbicacionTipo ubicacionTipo, Long ubicacionId);
}
