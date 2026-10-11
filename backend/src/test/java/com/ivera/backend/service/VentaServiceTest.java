package com.ivera.backend.service;

import com.ivera.backend.dto.VentaItemRequest;
import com.ivera.backend.dto.VentaRequest;
import com.ivera.backend.entity.*;
import com.ivera.backend.enums.UbicacionTipo;
import com.ivera.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private VentaDetalleRepository ventaDetalleRepository;
    @Mock private TiendaRepository tiendaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private UnidadMedidaRepository unidadMedidaRepository;
    @Mock private ProductoPresentacionRepository presentacionRepository;
    @Mock private StockRepository stockRepository;
    @Mock private MovimientoRepository movimientoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private VentaService ventaService;

    private final UnidadMedida unidad = unidad(1L, "unidad");
    private final UnidadMedida docena = unidad(2L, "docena");
    private final Producto producto = producto(10L, "Tornillo");
    private final Stock stock = stock(new BigDecimal("100"));

    @BeforeEach
    void configurarContextoYRepositorios() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("vendedor@ivera.pe", null)
        );
        Tienda tienda = new Tienda();
        tienda.setId(1L);
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Vendedor");
        when(tiendaRepository.findById(1L)).thenReturn(Optional.of(tienda));
        when(usuarioRepository.findByEmail("vendedor@ivera.pe")).thenReturn(Optional.of(usuario));
        lenient().when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta venta = invocation.getArgument(0);
            venta.setId(1L);
            return venta;
        });
        lenient().when(ventaDetalleRepository.save(any(VentaDetalle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(anyLong(), eq(UbicacionTipo.TIENDA), eq(1L)))
                .thenReturn(Optional.of(stock));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void persisteCantidadBaseParaVentaEnUnidadBase() {
        configurarProducto(producto, List.of(), Map.of(1L, unidad));

        ventaService.registrar(request(item(10L, 1L, "3")));

        assertThat(detallesGuardados()).extracting(VentaDetalle::getCantidadBase)
                .containsExactly(new BigDecimal("3"));
    }

    @Test
    void persisteCantidadBaseParaVentaEnDocenas() {
        ProductoPresentacion presentacion = presentacion(producto, docena, 12);
        configurarProducto(producto, List.of(presentacion), Map.of(2L, docena));

        ventaService.registrar(request(item(10L, 2L, "2")));

        assertThat(detallesGuardados()).extracting(VentaDetalle::getCantidadBase)
                .containsExactly(new BigDecimal("24"));
    }

    @Test
    void persisteCantidadesBaseEnVentaCombinada() {
        ProductoPresentacion presentacion = presentacion(producto, docena, 12);
        configurarProducto(producto, List.of(presentacion), Map.of(1L, unidad, 2L, docena));

        ventaService.registrar(request(item(10L, 1L, "5"), item(10L, 2L, "2")));

        assertThat(detallesGuardados()).extracting(VentaDetalle::getCantidadBase)
                .containsExactly(new BigDecimal("5"), new BigDecimal("24"));
        ArgumentCaptor<Movimiento> movimientos = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository, times(2)).save(movimientos.capture());
        assertThat(movimientos.getAllValues()).extracting(Movimiento::getCantidad)
                .containsExactly(new BigDecimal("5"), new BigDecimal("24"));
        assertThat(stock.getCantidad()).isEqualByComparingTo("71");
    }

    @Test
    void rechazaVentaConLineasRepetidasCuandoElTotalSuperaElStock() {
        stock.setCantidad(new BigDecimal("10"));
        configurarProducto(producto, List.of(), Map.of(1L, unidad));

        assertThatThrownBy(() -> ventaService.registrar(request(item(10L, 1L, "6"), item(10L, 1L, "6"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Disponible: 10")
                .hasMessageContaining("Requerido: 12");
        verify(ventaRepository, never()).save(any());
        verify(ventaDetalleRepository, never()).save(any());
        verify(movimientoRepository, never()).save(any());
        assertThat(stock.getCantidad()).isEqualByComparingTo("10");
    }

    @Test
    void bloqueaElProductoAntesDeConsultarStock() {
        configurarProducto(producto, List.of(), Map.of(1L, unidad));

        ventaService.registrar(request(item(10L, 1L, "1")));

        var orden = inOrder(productoRepository, stockRepository);
        orden.verify(productoRepository).findByIdForUpdate(10L);
        orden.verify(stockRepository).findByProductoIdAndUbicacionTipoAndUbicacionId(10L, UbicacionTipo.TIENDA, 1L);
    }

    @Test
    void conservaCantidadBaseCapturadaAunqueCambieElFactorPosteriormente() {
        ProductoPresentacion presentacion = presentacion(producto, docena, 12);
        configurarProducto(producto, List.of(presentacion), Map.of(2L, docena));

        ventaService.registrar(request(item(10L, 2L, "2")));
        presentacion.setFactorConversion(24);

        assertThat(detallesGuardados()).extracting(VentaDetalle::getCantidadBase)
                .containsExactly(new BigDecimal("24"));
    }

    @Test
    void noPersisteNadaCuandoUnaVentaFallaDuranteLaPrevalidacion() throws Exception {
        Producto sinStock = producto(20L, "Tuerca");
        configurarProducto(producto, List.of(), Map.of(1L, unidad));
        when(productoRepository.findById(20L)).thenReturn(Optional.of(sinStock));
        when(productoRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(sinStock));
        when(stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(eq(20L), eq(UbicacionTipo.TIENDA), eq(1L)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaService.registrar(request(item(10L, 1L, "1"), item(20L, 1L, "1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");
        verify(ventaRepository, never()).save(any());
        verify(ventaDetalleRepository, never()).save(any());
        verify(movimientoRepository, never()).save(any());
        assertThat(VentaService.class.getMethod("registrar", VentaRequest.class).isAnnotationPresent(Transactional.class)).isTrue();
    }

    private void configurarProducto(Producto producto, List<ProductoPresentacion> presentaciones, Map<Long, UnidadMedida> unidades) {
        when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(productoRepository.findByIdForUpdate(producto.getId())).thenReturn(Optional.of(producto));
        if (!presentaciones.isEmpty()) {
            when(presentacionRepository.findByProductoId(producto.getId())).thenReturn(presentaciones);
        }
        unidades.forEach((id, unidadMedida) -> when(unidadMedidaRepository.findById(id)).thenReturn(Optional.of(unidadMedida)));
    }

    private List<VentaDetalle> detallesGuardados() {
        ArgumentCaptor<VentaDetalle> detalles = ArgumentCaptor.forClass(VentaDetalle.class);
        verify(ventaDetalleRepository, atLeastOnce()).save(detalles.capture());
        return detalles.getAllValues();
    }

    private VentaRequest request(VentaItemRequest... items) {
        VentaRequest request = new VentaRequest();
        request.setTiendaId(1L);
        request.setItems(List.of(items));
        return request;
    }

    private VentaItemRequest item(Long productoId, Long unidadId, String cantidad) {
        VentaItemRequest item = new VentaItemRequest();
        item.setProductoId(productoId);
        item.setUnidadId(unidadId);
        item.setCantidad(new BigDecimal(cantidad));
        item.setPrecioUnitario(BigDecimal.ONE);
        return item;
    }

    private UnidadMedida unidad(Long id, String nombre) {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(id);
        unidad.setNombre(nombre);
        return unidad;
    }

    private Producto producto(Long id, String nombre) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setUnidadBase(unidad);
        return producto;
    }

    private ProductoPresentacion presentacion(Producto producto, UnidadMedida unidad, int factor) {
        ProductoPresentacion presentacion = new ProductoPresentacion();
        presentacion.setProducto(producto);
        presentacion.setUnidad(unidad);
        presentacion.setFactorConversion(factor);
        return presentacion;
    }

    private Stock stock(BigDecimal cantidad) {
        Stock stock = new Stock();
        stock.setCantidad(cantidad);
        return stock;
    }
}
