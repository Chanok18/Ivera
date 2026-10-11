package com.ivera.backend.service;

import com.ivera.backend.dto.StockBajoResponse;
import com.ivera.backend.entity.*;
import com.ivera.backend.enums.*;
import com.ivera.backend.exception.ReporteIncompletoException;
import com.ivera.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private StockRepository stockRepository;
    @Mock private ConteoDetalleRepository conteoDetalleRepository;
    @Mock private MovimientoRepository movimientoRepository;
    @Mock private VentaDetalleRepository ventaDetalleRepository;
    @Mock private TiendaRepository tiendaRepository;
    @Mock private AlmacenRepository almacenRepository;
    @InjectMocks private ReporteService reporteService;

    @Test
    void obtenerStockBajoUsaElUmbralIndicadoYElTotalAgregadoPorUbicacion() {
        StockBajoResponse respuesta = new StockBajoResponse(1L, "Tornillos", "123", new BigDecimal("9"));
        when(stockRepository.buscarStockBajo(new BigDecimal("10"))).thenReturn(List.of(respuesta));

        List<StockBajoResponse> resultado = reporteService.obtenerStockBajo(new BigDecimal("10"));

        assertThat(resultado).containsExactly(respuesta);
        verify(stockRepository).buscarStockBajo(new BigDecimal("10"));
    }

    @Test
    void obtenerStockBajoUsaDiezCuandoNoSeIndicaUmbral() {
        when(stockRepository.buscarStockBajo(BigDecimal.TEN)).thenReturn(List.of());

        reporteService.obtenerStockBajo(null);

        verify(stockRepository).buscarStockBajo(BigDecimal.TEN);
    }

    @Test
    void obtenerDiferenciasRechazaRangoInvertido() {
        assertThatThrownBy(() -> reporteService.obtenerDiferenciasInventario(
                LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha desde no puede ser posterior a la fecha hasta");
    }

    @Test
    void obtenerDiferenciasConsultaSoloElRangoYMapeaDiferenciasNoCero() {
        ConteoDetalle detalle = crearDetalleConteo(new BigDecimal("-2"));
        ConteoDetalle detalleSinDiferencia = crearDetalleConteo(BigDecimal.ZERO);
        Tienda tienda = new Tienda();
        tienda.setId(1L);
        tienda.setNombre("Romel");
        when(conteoDetalleRepository.buscarDiferencias(any(), any())).thenReturn(List.of(detalle, detalleSinDiferencia));
        when(tiendaRepository.findAllById(any())).thenReturn(List.of(tienda));

        var resultado = reporteService.obtenerDiferenciasInventario(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(resultado).singleElement().satisfies(respuesta -> {
            assertThat(respuesta.getDiferencia()).isEqualByComparingTo("-2");
            assertThat(respuesta.getUbicacionNombre()).isEqualTo("Romel");
        });
        verify(conteoDetalleRepository).buscarDiferencias(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 31, 23, 59, 59, 999_999_999));
    }

    @Test
    void obtenerMovimientosRechazaTiendaInexistente() {
        when(tiendaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> reporteService.obtenerMovimientos(99L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La tienda indicada no existe");
    }

    @Test
    void obtenerMovimientosRechazaTiendaNoPositiva() {
        assertThatThrownBy(() -> reporteService.obtenerMovimientos(0L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tiendaId debe ser un número positivo");
    }

    @Test
    void obtenerMovimientosConservaElOrdenDescendenteEntregadoPorLaConsulta() {
        Movimiento reciente = crearMovimiento(2L, LocalDateTime.of(2026, 2, 2, 10, 0));
        Movimiento antiguo = crearMovimiento(1L, LocalDateTime.of(2026, 2, 1, 10, 0));
        Tienda tienda = new Tienda();
        tienda.setId(1L);
        tienda.setNombre("Romel");
        when(tiendaRepository.existsById(1L)).thenReturn(true);
        when(movimientoRepository.buscarParaReporte(eq(1L), eq(UbicacionTipo.TIENDA), any(), any()))
                .thenReturn(List.of(reciente, antiguo));
        when(tiendaRepository.findAllById(any())).thenReturn(List.of(tienda));
        when(almacenRepository.findAllById(any())).thenReturn(List.of());

        var resultado = reporteService.obtenerMovimientos(1L, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 2));

        assertThat(resultado).extracting(respuesta -> respuesta.getId()).containsExactly(2L, 1L);
        verify(movimientoRepository).buscarParaReporte(
                1L, UbicacionTipo.TIENDA,
                LocalDateTime.of(2026, 2, 1, 0, 0),
                LocalDateTime.of(2026, 2, 2, 23, 59, 59, 999_999_999));
    }

    @Test
    void obtenerProductosMasVendidosSumaCantidadesBaseCuandoElHistorialEstaCompleto() {
        var respuesta = new com.ivera.backend.dto.ProductoMasVendidoResponse(1L, "Tornillo", new BigDecimal("29"));
        when(ventaDetalleRepository.existsByCantidadBaseIsNull()).thenReturn(false);
        when(ventaDetalleRepository.buscarProductosMasVendidos(any())).thenReturn(List.of(respuesta));

        var resultado = reporteService.obtenerProductosMasVendidos(5);

        assertThat(resultado).containsExactly(respuesta);
        verify(ventaDetalleRepository).buscarProductosMasVendidos(org.springframework.data.domain.PageRequest.of(0, 5));
    }

    @Test
    void obtenerProductosMasVendidosRechazaHistoricosSinCantidadBase() {
        when(ventaDetalleRepository.existsByCantidadBaseIsNull()).thenReturn(true);

        assertThatThrownBy(() -> reporteService.obtenerProductosMasVendidos(5))
                .isInstanceOf(ReporteIncompletoException.class)
                .hasMessage("No se puede calcular el reporte completo porque existen ventas históricas sin cantidad en unidad base");
        verify(ventaDetalleRepository, never()).buscarProductosMasVendidos(any());
    }

    private ConteoDetalle crearDetalleConteo(BigDecimal diferencia) {
        Producto producto = new Producto();
        producto.setId(10L);
        producto.setNombre("Perno");
        ConteoInventario conteo = new ConteoInventario();
        conteo.setId(5L);
        conteo.setUbicacionTipo(UbicacionTipo.TIENDA);
        conteo.setUbicacionId(1L);
        conteo.setFecha(LocalDateTime.of(2026, 1, 15, 12, 0));
        ConteoDetalle detalle = new ConteoDetalle();
        detalle.setConteo(conteo);
        detalle.setProducto(producto);
        detalle.setCantidadContada(new BigDecimal("8"));
        detalle.setCantidadSistema(new BigDecimal("10"));
        detalle.setDiferencia(diferencia);
        return detalle;
    }

    private Movimiento crearMovimiento(Long id, LocalDateTime fecha) {
        Producto producto = new Producto();
        producto.setId(10L);
        producto.setNombre("Perno");
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(1L);
        unidad.setNombre("unidad");
        Usuario usuario = new Usuario();
        usuario.setId(4L);
        usuario.setNombre("Ana");
        Movimiento movimiento = new Movimiento();
        movimiento.setId(id);
        movimiento.setProducto(producto);
        movimiento.setUnidad(unidad);
        movimiento.setUsuario(usuario);
        movimiento.setTipo(MovimientoTipo.ENTRADA);
        movimiento.setCantidad(BigDecimal.ONE);
        movimiento.setUbicacionOrigenTipo(UbicacionTipo.TIENDA);
        movimiento.setUbicacionOrigenId(1L);
        movimiento.setEstado(MovimientoEstado.APROBADO);
        movimiento.setFecha(fecha);
        return movimiento;
    }
}
