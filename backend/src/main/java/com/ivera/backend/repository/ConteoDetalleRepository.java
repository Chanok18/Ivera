package com.ivera.backend.repository;

import com.ivera.backend.entity.ConteoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConteoDetalleRepository extends JpaRepository<ConteoDetalle, Long> {
    List<ConteoDetalle> findByConteoId(Long conteoId);
    Optional<ConteoDetalle> findByConteoIdAndProductoId(Long conteoId, Long productoId);
    void deleteByConteoId(Long conteoId);

    @Query("""
            select d from ConteoDetalle d
            join fetch d.conteo c
            join fetch d.producto
            where d.diferencia <> 0
              and (:desde is null or c.fecha >= :desde)
              and (:hasta is null or c.fecha <= :hasta)
            """)
    List<ConteoDetalle> buscarDiferencias(@Param("desde") LocalDateTime desde,
                                          @Param("hasta") LocalDateTime hasta);

    @Query("""
            select d from ConteoDetalle d
            join fetch d.conteo c
            join fetch d.producto
            where d.diferencia <> 0
              and c.ubicacionTipo = :tipo and c.ubicacionId = :ubicacionId
              and (:desde is null or c.fecha >= :desde)
              and (:hasta is null or c.fecha <= :hasta)
            """)
    List<ConteoDetalle> buscarDiferenciasPorUbicacion(@Param("tipo") com.ivera.backend.enums.UbicacionTipo tipo,
                                                       @Param("ubicacionId") Long ubicacionId,
                                                       @Param("desde") LocalDateTime desde,
                                                       @Param("hasta") LocalDateTime hasta);
}
