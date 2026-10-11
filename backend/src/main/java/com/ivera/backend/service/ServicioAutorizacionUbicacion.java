package com.ivera.backend.service;

import com.ivera.backend.entity.Usuario;
import com.ivera.backend.enums.AccionAlmacen;
import com.ivera.backend.enums.EstadoAutorizacionAlmacen;
import com.ivera.backend.enums.Rol;
import com.ivera.backend.enums.UbicacionTipo;
import com.ivera.backend.repository.AutorizacionAlmacenTrabajadorRepository;
import com.ivera.backend.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ServicioAutorizacionUbicacion {
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionAlmacenTrabajadorRepository autorizacionRepository;

    public ServicioAutorizacionUbicacion(UsuarioRepository usuarioRepository,
                                         AutorizacionAlmacenTrabajadorRepository autorizacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.autorizacionRepository = autorizacionRepository;
    }

    public Usuario obtenerUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado: " + email));
    }

    public boolean esAdministrador() {
        return obtenerUsuarioActual().getRol() == Rol.ADMINISTRADOR;
    }

    public void verificarTienda(Long tiendaId) {
        Usuario usuario = obtenerUsuarioActual();
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            return;
        }
        if (usuario.getTienda() == null || !usuario.getTienda().getId().equals(tiendaId)) {
            throw new AccessDeniedException("No tiene permiso para operar en esta tienda");
        }
    }

    public void verificarUbicacion(UbicacionTipo tipo, Long ubicacionId, AccionAlmacen accionAlmacen) {
        if (tipo == UbicacionTipo.TIENDA) {
            verificarTienda(ubicacionId);
            return;
        }

        Usuario usuario = obtenerUsuarioActual();
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            return;
        }
        if (!autorizacionRepository.existsByTrabajadorIdAndAlmacenIdAndAccionAndEstado(
                usuario.getId(), ubicacionId, accionAlmacen, EstadoAutorizacionAlmacen.ACTIVA)) {
            throw new AccessDeniedException("No tiene permiso para realizar esta operación en el almacén");
        }
    }

    public Long resolverTiendaParaConsulta(Long tiendaIdSolicitada) {
        Usuario usuario = obtenerUsuarioActual();
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            return tiendaIdSolicitada;
        }
        if (usuario.getTienda() == null) {
            throw new AccessDeniedException("El trabajador no tiene una tienda asignada");
        }
        if (tiendaIdSolicitada != null && !usuario.getTienda().getId().equals(tiendaIdSolicitada)) {
            throw new AccessDeniedException("No tiene permiso para consultar otra tienda");
        }
        return usuario.getTienda().getId();
    }
}
