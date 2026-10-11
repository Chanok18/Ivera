package com.ivera.backend.service;

import com.ivera.backend.entity.*;
import com.ivera.backend.enums.*;
import com.ivera.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

    @Mock private MovimientoRepository movimientoRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private UnidadMedidaRepository unidadMedidaRepository;
    @Mock private ProductoPresentacionRepository presentacionRepository;
    @Mock private TiendaRepository tiendaRepository;
    @Mock private AlmacenRepository almacenRepository;
    @Mock private StockRepository stockRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private MovimientoService movimientoService;

    private Movimiento movimiento;
    private Stock stock;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@ivera.pe", null)
        );
        Producto producto = new Producto();
        producto.setId(10L);
        producto.setNombre("Tornillo");
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(1L);
        unidad.setNombre("unidad");
        Usuario autor = new Usuario();
        autor.setId(1L);
        autor.setNombre("Trabajador");
        Usuario admin = new Usuario();
        admin.setId(2L);
        admin.setNombre("Administrador");
        Tienda tienda = new Tienda();
        tienda.setId(1L);
        tienda.setNombre("Romel");
        movimiento = new Movimiento();
        movimiento.setId(5L);
        movimiento.setProducto(producto);
        movimiento.setUnidad(unidad);
        movimiento.setUsuario(autor);
        movimiento.setTipo(MovimientoTipo.AJUSTE);
        movimiento.setCantidad(BigDecimal.ONE);
        movimiento.setUbicacionOrigenTipo(UbicacionTipo.TIENDA);
        movimiento.setUbicacionOrigenId(1L);
        movimiento.setEstado(MovimientoEstado.PENDIENTE);
        stock = new Stock();
        stock.setCantidad(new BigDecimal("5"));

        when(movimientoRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(movimiento));
        lenient().when(productoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(producto));
        lenient().when(usuarioRepository.findByEmail("admin@ivera.pe")).thenReturn(Optional.of(admin));
        lenient().when(stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(10L, UbicacionTipo.TIENDA, 1L))
                .thenReturn(Optional.of(stock));
        lenient().when(tiendaRepository.findById(1L)).thenReturn(Optional.of(tienda));
        lenient().when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void apruebaUnaSolaVezBajoBloqueoYActualizaStockUnaVez() {
        movimientoService.aprobar(5L);

        assertThat(movimiento.getEstado()).isEqualTo(MovimientoEstado.APROBADO);
        assertThat(stock.getCantidad()).isEqualByComparingTo("6");
        assertThat(movimiento.getAprobadoPor().getNombre()).isEqualTo("Administrador");
        assertThatThrownBy(() -> movimientoService.aprobar(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no se encuentra en estado PENDIENTE");
        verify(movimientoRepository, times(2)).findByIdForUpdate(5L);
        verify(stockRepository, times(1)).save(stock);
        verify(movimientoRepository, times(1)).save(movimiento);
    }

    @Test
    void noApruebaUnMovimientoRechazado() {
        movimiento.setEstado(MovimientoEstado.RECHAZADO);

        assertThatThrownBy(() -> movimientoService.aprobar(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RECHAZADO");
        verify(stockRepository, never()).save(any());
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void rechazaUnaSolaVezBajoBloqueoSinModificarStock() {
        movimientoService.rechazar(5L);

        assertThat(movimiento.getEstado()).isEqualTo(MovimientoEstado.RECHAZADO);
        assertThatThrownBy(() -> movimientoService.rechazar(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no se encuentra en estado PENDIENTE");
        verify(movimientoRepository, times(2)).findByIdForUpdate(5L);
        verify(stockRepository, never()).save(any());
        verify(movimientoRepository, times(1)).save(movimiento);
    }
}
