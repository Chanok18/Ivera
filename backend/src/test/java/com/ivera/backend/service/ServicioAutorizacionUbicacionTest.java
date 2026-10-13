package com.ivera.backend.service;

import com.ivera.backend.entity.Tienda;
import com.ivera.backend.entity.Usuario;
import com.ivera.backend.enums.*;
import com.ivera.backend.repository.AutorizacionAlmacenTrabajadorRepository;
import com.ivera.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioAutorizacionUbicacionTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AutorizacionAlmacenTrabajadorRepository autorizacionRepository;
    @InjectMocks private ServicioAutorizacionUbicacion autorizacionUbicacion;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("usuario@ivera.pe", null));
        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setRol(Rol.TRABAJADOR);
        Tienda tienda = new Tienda();
        tienda.setId(1L);
        usuario.setTienda(tienda);
        when(usuarioRepository.findByEmail("usuario@ivera.pe")).thenReturn(Optional.of(usuario));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void administradorTieneAccesoGlobal() {
        usuario.setRol(Rol.ADMINISTRADOR);

        autorizacionUbicacion.verificarTienda(2L);
        autorizacionUbicacion.verificarUbicacion(UbicacionTipo.ALMACEN, 7L, AccionAlmacen.AJUSTE);

        verifyNoInteractions(autorizacionRepository);
    }

    @Test
    void trabajadorOperaSoloEnSuTienda() {
        autorizacionUbicacion.verificarTienda(1L);

        assertThatThrownBy(() -> autorizacionUbicacion.verificarTienda(2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void trabajadorSinPermisoDeAlmacenEsRechazado() {
        when(autorizacionRepository.existsByTrabajadorIdAndAlmacenIdAndAccionAndEstado(
                10L, 3L, AccionAlmacen.CONTEO, EstadoAutorizacionAlmacen.ACTIVA)).thenReturn(false);

        assertThatThrownBy(() -> autorizacionUbicacion.verificarUbicacion(UbicacionTipo.ALMACEN, 3L, AccionAlmacen.CONTEO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void trabajadorConPermisoEspecificoOperaEnAlmacen() {
        when(autorizacionRepository.existsByTrabajadorIdAndAlmacenIdAndAccionAndEstado(
                10L, 3L, AccionAlmacen.TRASLADO_ORIGEN, EstadoAutorizacionAlmacen.ACTIVA)).thenReturn(true);

        autorizacionUbicacion.verificarUbicacion(UbicacionTipo.ALMACEN, 3L, AccionAlmacen.TRASLADO_ORIGEN);
    }

    @Test
    void revocacionImpideOperacionesPosterioresEnAlmacen() {
        when(autorizacionRepository.existsByTrabajadorIdAndAlmacenIdAndAccionAndEstado(
                10L, 3L, AccionAlmacen.CONTEO, EstadoAutorizacionAlmacen.ACTIVA)).thenReturn(false);

        assertThatThrownBy(() -> autorizacionUbicacion.verificarUbicacion(UbicacionTipo.ALMACEN, 3L, AccionAlmacen.CONTEO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cambioDeTiendaEliminaAccesoOperativoAnterior() {
        usuario.getTienda().setId(2L);

        assertThatThrownBy(() -> autorizacionUbicacion.verificarTienda(1L))
                .isInstanceOf(AccessDeniedException.class);
        autorizacionUbicacion.verificarTienda(2L);
    }

    @Test
    void reporteDeOtraTiendaEsRechazado() {
        assertThatThrownBy(() -> autorizacionUbicacion.resolverTiendaParaConsulta(2L))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(autorizacionUbicacion.resolverTiendaParaConsulta(null)).isEqualTo(1L);
    }
}
