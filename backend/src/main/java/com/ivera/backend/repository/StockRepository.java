package com.ivera.backend.repository;

import com.ivera.backend.entity.Stock;
import com.ivera.backend.enums.UbicacionTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ivera.backend.dto.StockBajoResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByProductoId(Long productoId);
    Optional<Stock> findByProductoIdAndUbicacionTipoAndUbicacionId(Long productoId, UbicacionTipo ubicacionTipo, Long ubicacionId);

    @Query("""
            select new com.ivera.backend.dto.StockBajoResponse(p.id, p.nombre, p.codigoBarras, coalesce(sum(s.cantidad), 0))
            from Producto p left join Stock s on s.producto = p
            group by p.id, p.nombre, p.codigoBarras
            having coalesce(sum(s.cantidad), 0) <= :umbral
            """)
    List<StockBajoResponse> buscarStockBajo(@Param("umbral") BigDecimal umbral);
}
