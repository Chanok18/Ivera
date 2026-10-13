package com.ivera.backend.service;

import com.ivera.backend.dto.AutorizacionAlmacenRequest;
import com.ivera.backend.entity.Almacen;
import com.ivera.backend.entity.AutorizacionAlmacenTrabajador;
import com.ivera.backend.entity.Usuario;
import com.ivera.backend.enums.*;
import com.ivera.backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutorizacionAlmacenServiceTest {
    @Mock private AutorizacionAlmacenTrabajadorRepository autorizacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AlmacenRepository almacenRepository;
    @Mock private ServicioAutorizacionUbicacion autorizacionUbicacion;
    @InjectMocks private AutorizacionAlmacenService autorizacionService;

    @Test
    void administradorOtorgaYRevocaPermisoAuditable() {
        Usuario admin = usuario(1L, Rol.ADMINISTRADOR, "Admin");
        Usuario trabajador = usuario(2L, Rol.TRABAJADOR, "Ana");
        Almacen almacen = new Almacen();
        almacen.setId(3L);
        almacen.setNombre("Central");
        AutorizacionAlmacenRequest request = new AutorizacionAlmacenRequest();
        request.setTrabajadorId(2L);
        request.setAlmacenId(3L);
        request.setAccion(AccionAlmacen.CONTEO);
        when(autorizacionUbicacion.obtenerUsuarioActual()).thenReturn(admin);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(trabajador));
        when(almacenRepository.findById(3L)).thenReturn(Optional.of(almacen));
        when(autorizacionRepository.save(any())).thenAnswer(invocation -> {
            AutorizacionAlmacenTrabajador autorizacion = invocation.getArgument(0);
            autorizacion.setId(8L);
            return autorizacion;
        });

        var otorgada = autorizacionService.otorgar(request);
        when(autorizacionRepository.findById(8L)).thenReturn(Optional.of(crearAutorizacion(trabajador, almacen, admin)));
        var revocada = autorizacionService.revocar(8L);

        assertThat(otorgada.getEstado()).isEqualTo(EstadoAutorizacionAlmacen.ACTIVA);
        assertThat(otorgada.getOtorgadoPorId()).isEqualTo(1L);
        assertThat(revocada.getEstado()).isEqualTo(EstadoAutorizacionAlmacen.REVOCADA);
        assertThat(revocada.getRevocadoPorId()).isEqualTo(1L);
    }

    @Test
    void trabajadorNoPuedeOtorgarsePermisos() {
        when(autorizacionUbicacion.obtenerUsuarioActual()).thenReturn(usuario(2L, Rol.TRABAJADOR, "Ana"));

        assertThatThrownBy(() -> autorizacionService.otorgar(new AutorizacionAlmacenRequest()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(usuarioRepository, almacenRepository, autorizacionRepository);
    }

    private Usuario usuario(Long id, Rol rol, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setNombre(nombre);
        return usuario;
    }

    private AutorizacionAlmacenTrabajador crearAutorizacion(Usuario trabajador, Almacen almacen, Usuario admin) {
        AutorizacionAlmacenTrabajador autorizacion = new AutorizacionAlmacenTrabajador();
        autorizacion.setId(8L);
        autorizacion.setTrabajador(trabajador);
        autorizacion.setAlmacen(almacen);
        autorizacion.setAccion(AccionAlmacen.CONTEO);
        autorizacion.setEstado(EstadoAutorizacionAlmacen.ACTIVA);
        autorizacion.setOtorgadoPor(admin);
        return autorizacion;
    }
}
