package com.ivera.backend.repository;

import com.ivera.backend.entity.AutorizacionAlmacenTrabajador;
import com.ivera.backend.enums.AccionAlmacen;
import com.ivera.backend.enums.EstadoAutorizacionAlmacen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AutorizacionAlmacenTrabajadorRepository extends JpaRepository<AutorizacionAlmacenTrabajador, Long> {
    boolean existsByTrabajadorIdAndAlmacenIdAndAccionAndEstado(Long trabajadorId, Long almacenId,
                                                                AccionAlmacen accion, EstadoAutorizacionAlmacen estado);
    Optional<AutorizacionAlmacenTrabajador> findByTrabajadorIdAndAlmacenIdAndAccionAndEstado(Long trabajadorId,
                                                                                                Long almacenId,
                                                                                                AccionAlmacen accion,
                                                                                                EstadoAutorizacionAlmacen estado);
    List<AutorizacionAlmacenTrabajador> findByTrabajadorIdOrderByOtorgadoEnDesc(Long trabajadorId);
    List<AutorizacionAlmacenTrabajador> findAllByOrderByOtorgadoEnDesc();
}
