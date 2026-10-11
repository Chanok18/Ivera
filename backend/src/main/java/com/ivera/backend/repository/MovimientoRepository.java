package com.ivera.backend.repository;

import com.ivera.backend.entity.Movimiento;
import com.ivera.backend.enums.UbicacionTipo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Movimiento m where m.id = :id")
    Optional<Movimiento> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select m from Movimiento m
            join fetch m.producto
            join fetch m.unidad
            join fetch m.usuario
            left join fetch m.aprobadoPor
            where (:tiendaId is null or
                   (m.ubicacionOrigenTipo = :tipoTienda and m.ubicacionOrigenId = :tiendaId) or
                   (m.ubicacionDestinoTipo = :tipoTienda and m.ubicacionDestinoId = :tiendaId))
              and (:desde is null or m.fecha >= :desde)
              and (:hasta is null or m.fecha <= :hasta)
            order by m.fecha desc
            """)
    List<Movimiento> buscarParaReporte(@Param("tiendaId") Long tiendaId,
                                        @Param("tipoTienda") UbicacionTipo tipoTienda,
                                        @Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta);
}
