package com.ivera.backend.service;

import com.ivera.backend.dto.AutorizacionAlmacenRequest;
import com.ivera.backend.dto.AutorizacionAlmacenResponse;
import com.ivera.backend.entity.Almacen;
import com.ivera.backend.entity.AutorizacionAlmacenTrabajador;
import com.ivera.backend.entity.Usuario;
import com.ivera.backend.enums.EstadoAutorizacionAlmacen;
import com.ivera.backend.enums.Rol;
import com.ivera.backend.repository.AlmacenRepository;
import com.ivera.backend.repository.AutorizacionAlmacenTrabajadorRepository;
import com.ivera.backend.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AutorizacionAlmacenService {
    private final AutorizacionAlmacenTrabajadorRepository autorizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlmacenRepository almacenRepository;
    private final ServicioAutorizacionUbicacion autorizacionUbicacion;

    public AutorizacionAlmacenService(AutorizacionAlmacenTrabajadorRepository autorizacionRepository,
                                      UsuarioRepository usuarioRepository,
                                      AlmacenRepository almacenRepository,
                                      ServicioAutorizacionUbicacion autorizacionUbicacion) {
        this.autorizacionRepository = autorizacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.almacenRepository = almacenRepository;
        this.autorizacionUbicacion = autorizacionUbicacion;
    }

    @Transactional(readOnly = true)
    public List<AutorizacionAlmacenResponse> listar(Long trabajadorId) {
        verificarAdministrador();
        List<AutorizacionAlmacenTrabajador> autorizaciones = trabajadorId == null
                ? autorizacionRepository.findAllByOrderByOtorgadoEnDesc()
                : autorizacionRepository.findByTrabajadorIdOrderByOtorgadoEnDesc(trabajadorId);
        return autorizaciones.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AutorizacionAlmacenResponse otorgar(AutorizacionAlmacenRequest request) {
        Usuario admin = verificarAdministrador();
        Usuario trabajador = usuarioRepository.findById(request.getTrabajadorId())
                .orElseThrow(() -> new EntityNotFoundException("Trabajador no encontrado: " + request.getTrabajadorId()));
        if (trabajador.getRol() != Rol.TRABAJADOR) {
            throw new IllegalArgumentException("Solo se pueden otorgar permisos de almacén a usuarios TRABAJADOR");
        }
        Almacen almacen = almacenRepository.findById(request.getAlmacenId())
                .orElseThrow(() -> new EntityNotFoundException("Almacén no encontrado: " + request.getAlmacenId()));
        if (autorizacionRepository.existsByTrabajadorIdAndAlmacenIdAndAccionAndEstado(
                trabajador.getId(), almacen.getId(), request.getAccion(), EstadoAutorizacionAlmacen.ACTIVA)) {
            throw new IllegalStateException("Ya existe una autorización activa para este trabajador, almacén y acción");
        }

        AutorizacionAlmacenTrabajador autorizacion = new AutorizacionAlmacenTrabajador();
        autorizacion.setTrabajador(trabajador);
        autorizacion.setAlmacen(almacen);
        autorizacion.setAccion(request.getAccion());
        autorizacion.setEstado(EstadoAutorizacionAlmacen.ACTIVA);
        autorizacion.setOtorgadoPor(admin);
        return toResponse(autorizacionRepository.save(autorizacion));
    }

    @Transactional
    public AutorizacionAlmacenResponse revocar(Long id) {
        Usuario admin = verificarAdministrador();
        AutorizacionAlmacenTrabajador autorizacion = autorizacionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Autorización no encontrada: " + id));
        if (autorizacion.getEstado() != EstadoAutorizacionAlmacen.ACTIVA) {
            throw new IllegalStateException("La autorización ya se encuentra revocada");
        }
        autorizacion.setEstado(EstadoAutorizacionAlmacen.REVOCADA);
        autorizacion.setRevocadoPor(admin);
        autorizacion.setRevocadoEn(LocalDateTime.now());
        return toResponse(autorizacionRepository.save(autorizacion));
    }

    private Usuario verificarAdministrador() {
        Usuario usuario = autorizacionUbicacion.obtenerUsuarioActual();
        if (usuario.getRol() != Rol.ADMINISTRADOR) {
            throw new AccessDeniedException("Solo un administrador puede gestionar autorizaciones de almacén");
        }
        return usuario;
    }

    private AutorizacionAlmacenResponse toResponse(AutorizacionAlmacenTrabajador a) {
        AutorizacionAlmacenResponse res = new AutorizacionAlmacenResponse();
        res.setId(a.getId());
        res.setTrabajadorId(a.getTrabajador().getId());
        res.setTrabajadorNombre(a.getTrabajador().getNombre());
        res.setAlmacenId(a.getAlmacen().getId());
        res.setAlmacenNombre(a.getAlmacen().getNombre());
        res.setAccion(a.getAccion());
        res.setEstado(a.getEstado());
        res.setOtorgadoPorId(a.getOtorgadoPor().getId());
        res.setOtorgadoPorNombre(a.getOtorgadoPor().getNombre());
        res.setOtorgadoEn(a.getOtorgadoEn());
        res.setRevocadoPorId(a.getRevocadoPor() != null ? a.getRevocadoPor().getId() : null);
        res.setRevocadoPorNombre(a.getRevocadoPor() != null ? a.getRevocadoPor().getNombre() : null);
        res.setRevocadoEn(a.getRevocadoEn());
        return res;
    }
}
