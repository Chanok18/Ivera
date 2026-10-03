package com.ivera.backend.repository;

import com.ivera.backend.entity.ConteoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConteoDetalleRepository extends JpaRepository<ConteoDetalle, Long> {
    List<ConteoDetalle> findByConteoId(Long conteoId);
    Optional<ConteoDetalle> findByConteoIdAndProductoId(Long conteoId, Long productoId);
    void deleteByConteoId(Long conteoId);
}
