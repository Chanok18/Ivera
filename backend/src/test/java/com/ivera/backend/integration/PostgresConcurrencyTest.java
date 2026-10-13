package com.ivera.backend.integration;

import com.ivera.backend.dto.AutorizacionAlmacenRequest;
import com.ivera.backend.dto.MovimientoRequest;
import com.ivera.backend.dto.VentaItemRequest;
import com.ivera.backend.dto.VentaRequest;
import com.ivera.backend.entity.Almacen;
import com.ivera.backend.entity.Movimiento;
import com.ivera.backend.entity.Producto;
import com.ivera.backend.entity.ProductoPresentacion;
import com.ivera.backend.entity.Stock;
import com.ivera.backend.entity.Tienda;
import com.ivera.backend.entity.UnidadMedida;
import com.ivera.backend.entity.Usuario;
import com.ivera.backend.enums.AccionAlmacen;
import com.ivera.backend.enums.EstadoAutorizacionAlmacen;
import com.ivera.backend.enums.MovimientoEstado;
import com.ivera.backend.enums.MovimientoTipo;
import com.ivera.backend.enums.Rol;
import com.ivera.backend.enums.UbicacionTipo;
import com.ivera.backend.repository.AlmacenRepository;
import com.ivera.backend.repository.AutorizacionAlmacenTrabajadorRepository;
import com.ivera.backend.repository.MovimientoRepository;
import com.ivera.backend.repository.ProductoPresentacionRepository;
import com.ivera.backend.repository.ProductoRepository;
import com.ivera.backend.repository.StockRepository;
import com.ivera.backend.repository.TiendaRepository;
import com.ivera.backend.repository.UnidadMedidaRepository;
import com.ivera.backend.repository.UsuarioRepository;
import com.ivera.backend.service.AutorizacionAlmacenService;
import com.ivera.backend.service.MovimientoService;
import com.ivera.backend.service.VentaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Executes only when explicitly enabled against the local isolated PostgreSQL database. */
@SpringBootTest
@ActiveProfiles("postgres-integration")
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_INTEGRATION", matches = "true")
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class PostgresConcurrencyTest {
    @Autowired private VentaService ventaService;
    @Autowired private MovimientoService movimientoService;
    @Autowired private AutorizacionAlmacenService autorizacionAlmacenService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProductoPresentacionRepository presentacionRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private MovimientoRepository movimientoRepository;
    @Autowired private UnidadMedidaRepository unidadRepository;
    @Autowired private TiendaRepository tiendaRepository;
    @Autowired private AlmacenRepository almacenRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AutorizacionAlmacenTrabajadorRepository autorizacionRepository;
    @Autowired private JdbcTemplate jdbc;

    private String prefix;

    @BeforeEach
    void preparar() {
        prefix = "pgit-" + UUID.randomUUID();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        jdbc.update("delete from autorizacion_almacen_trabajador where usuario_id in (select id from usuario where email like ?) or otorgado_por_usuario_id in (select id from usuario where email like ?) or revocado_por_usuario_id in (select id from usuario where email like ?)", prefix + "%", prefix + "%", prefix + "%");
        jdbc.update("delete from movimiento where usuario_id in (select id from usuario where email like ?) or aprobado_por in (select id from usuario where email like ?)", prefix + "%", prefix + "%");
        jdbc.update("delete from venta_detalle where venta_id in (select id from venta where usuario_id in (select id from usuario where email like ?))", prefix + "%");
        jdbc.update("delete from venta where usuario_id in (select id from usuario where email like ?)", prefix + "%");
        jdbc.update("delete from stock where producto_id in (select id from producto where nombre like ?)", prefix + "%");
        jdbc.update("delete from producto_presentacion where producto_id in (select id from producto where nombre like ?)", prefix + "%");
        jdbc.update("delete from producto where nombre like ?", prefix + "%");
        jdbc.update("delete from usuario where email like ?", prefix + "%");
        jdbc.update("delete from almacen where nombre like ?", prefix + "%");
        jdbc.update("delete from tienda where nombre like ?", prefix + "%");
        jdbc.update("delete from unidad_medida where nombre like ?", prefix + "%");
    }

    @Test
    void dosVentasSimultaneasNoSobrevenden() throws Exception {
        Datos d = datosBase(new BigDecimal("10"));
        Resultado[] resultados = ejecutarEnParalelo(d.trabajador.getEmail(), () -> ventaService.registrar(venta(d, "8")),
                d.trabajador.getEmail(), () -> ventaService.registrar(venta(d, "8")));

        assertThat(exitos(resultados)).isEqualTo(1);
        assertThat(falloDeTipo(resultados, IllegalArgumentException.class)).isTrue();
        assertThat(stock(d, d.tiendaA).getCantidad()).isEqualByComparingTo("2");
        assertThat(contar("select count(*) from venta where usuario_id = ?", d.trabajador.getId())).isEqualTo(1);
        assertThat(contar("select count(*) from movimiento where usuario_id = ? and tipo = 'SALIDA'", d.trabajador.getId())).isEqualTo(1);
    }

    @Test
    void dosAprobacionesSimultaneasAplicanUnaSolaVez() throws Exception {
        Datos d = datosBase(new BigDecimal("10"));
        Movimiento pendiente = trasladoPendiente(d, d.tiendaA, d.tiendaB, "5");
        Resultado[] resultados = ejecutarEnParalelo(d.adminA.getEmail(), () -> movimientoService.aprobar(pendiente.getId()),
                d.adminB.getEmail(), () -> movimientoService.aprobar(pendiente.getId()));

        assertThat(exitos(resultados)).isEqualTo(1);
        assertThat(falloDeTipo(resultados, IllegalStateException.class)).isTrue();
        assertThat(movimientoRepository.findById(pendiente.getId()).orElseThrow().getEstado()).isEqualTo(MovimientoEstado.APROBADO);
        assertThat(stock(d, d.tiendaA).getCantidad()).isEqualByComparingTo("5");
        assertThat(stock(d, d.tiendaB).getCantidad()).isEqualByComparingTo("5");
    }

    @Test
    void aprobarYRechazarSimultaneosDejanUnUnicoEstadoConsistente() throws Exception {
        Datos d = datosBase(new BigDecimal("10"));
        Movimiento pendiente = trasladoPendiente(d, d.tiendaA, d.tiendaB, "5");
        Resultado[] resultados = ejecutarEnParalelo(d.adminA.getEmail(), () -> movimientoService.aprobar(pendiente.getId()),
                d.adminB.getEmail(), () -> movimientoService.rechazar(pendiente.getId()));

        assertThat(exitos(resultados)).isEqualTo(1);
        assertThat(falloDeTipo(resultados, IllegalStateException.class)).isTrue();
        Movimiento finalizado = movimientoRepository.findById(pendiente.getId()).orElseThrow();
        if (finalizado.getEstado() == MovimientoEstado.APROBADO) {
            assertThat(stock(d, d.tiendaA).getCantidad()).isEqualByComparingTo("5");
            assertThat(stock(d, d.tiendaB).getCantidad()).isEqualByComparingTo("5");
        } else {
            assertThat(finalizado.getEstado()).isEqualTo(MovimientoEstado.RECHAZADO);
            assertThat(stock(d, d.tiendaA).getCantidad()).isEqualByComparingTo("10");
            assertThat(stock(d, d.tiendaB)).isNull();
        }
    }

    @Test
    void ventaContraTrasladoNoDejaStockNegativo() throws Exception {
        Datos d = datosBase(new BigDecimal("10"));
        Movimiento pendiente = trasladoPendiente(d, d.tiendaA, d.tiendaB, "8");
        Resultado[] resultados = ejecutarEnParalelo(d.trabajador.getEmail(), () -> ventaService.registrar(venta(d, "8")),
                d.adminA.getEmail(), () -> movimientoService.aprobar(pendiente.getId()));

        assertThat(exitos(resultados)).isEqualTo(1);
        assertThat(falloDeTipo(resultados, IllegalArgumentException.class, IllegalStateException.class)).isTrue();
        BigDecimal origen = stock(d, d.tiendaA).getCantidad();
        BigDecimal destino = stock(d, d.tiendaB) == null ? BigDecimal.ZERO : stock(d, d.tiendaB).getCantidad();
        assertThat(origen).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(destino).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(origen.add(destino)).isEqualByComparingTo("10");
    }

    @Test
    void entradasConcurrentesSinStockCreanUnaSolaFila() throws Exception {
        Datos d = datosBase(null);
        Resultado[] resultados = ejecutarEnParalelo(d.adminA.getEmail(), () -> movimientoService.registrar(entrada(d, "3")),
                d.adminB.getEmail(), () -> movimientoService.registrar(entrada(d, "4")));

        assertThat(exitos(resultados)).isEqualTo(2);
        assertThat(contar("select count(*) from stock where producto_id = ? and ubicacion_tipo = 'TIENDA' and ubicacion_id = ?", d.producto.getId(), d.tiendaA.getId())).isEqualTo(1);
        assertThat(stock(d, d.tiendaA).getCantidad()).isEqualByComparingTo("7");
    }

    @Test
    void trasladosEntreMultiplesUbicacionesNoPersistenDeadlock() throws Exception {
        Datos d = datosBase(new BigDecimal("10"));
        guardarStock(d, d.tiendaB, "10");
        Movimiento ida = trasladoPendiente(d, d.tiendaA, d.tiendaB, "2");
        Movimiento vuelta = trasladoPendiente(d, d.tiendaB, d.tiendaA, "3");
        Resultado[] resultados = ejecutarEnParalelo(d.adminA.getEmail(), () -> movimientoService.aprobar(ida.getId()),
                d.adminB.getEmail(), () -> movimientoService.aprobar(vuelta.getId()));

        assertThat(exitos(resultados)).isEqualTo(2);
        assertThat(stock(d, d.tiendaA).getCantidad()).isEqualByComparingTo("11");
        assertThat(stock(d, d.tiendaB).getCantidad()).isEqualByComparingTo("9");
    }

    @Test
    void cantidadBaseEsInmutableAnteCambioDeFactor() throws Exception {
        Datos d = datosBase(new BigDecimal("20"));
        UnidadMedida docena = unidad(prefix + "-docena");
        ProductoPresentacion presentacion = new ProductoPresentacion();
        presentacion.setProducto(d.producto);
        presentacion.setUnidad(docena);
        presentacion.setFactorConversion(12);
        presentacionRepository.saveAndFlush(presentacion);

        var respuesta = como(d.trabajador.getEmail(), () -> ventaService.registrar(venta(d, "1", docena.getId())));
        presentacion.setFactorConversion(24);
        presentacionRepository.saveAndFlush(presentacion);
        BigDecimal cantidadBase = jdbc.queryForObject("select cantidad_base from venta_detalle where venta_id = ?", BigDecimal.class, respuesta.getId());

        assertThat(cantidadBase).isEqualByComparingTo("12");
    }

    @Test
    void autorizacionActivaRevocadaEIndiceParcialSeVerificanEnPostgres() throws Exception {
        Datos d = datosBase(new BigDecimal("10"));
        AutorizacionAlmacenRequest solicitud = autorizacion(d);
        Resultado[] resultados = ejecutarEnParalelo(d.adminA.getEmail(), () -> autorizacionAlmacenService.otorgar(solicitud),
                d.adminB.getEmail(), () -> autorizacionAlmacenService.otorgar(solicitud));

        assertThat(exitos(resultados)).isEqualTo(1);
        assertThat(falloDeTipo(resultados, org.springframework.dao.DataIntegrityViolationException.class)).isTrue();
        assertThat(contar("select count(*) from autorizacion_almacen_trabajador where usuario_id = ? and almacen_id = ? and accion = 'AJUSTE' and estado = 'ACTIVA'", d.trabajador.getId(), d.almacen.getId())).isEqualTo(1);
        como(d.trabajador.getEmail(), () -> movimientoService.registrar(ajusteAlmacen(d, "1")));
        Long id = jdbc.queryForObject("select id from autorizacion_almacen_trabajador where usuario_id = ? and almacen_id = ? and accion = 'AJUSTE' and estado = 'ACTIVA'", Long.class, d.trabajador.getId(), d.almacen.getId());
        como(d.adminA.getEmail(), () -> autorizacionAlmacenService.revocar(id));

        assertThatThrownBy(() -> como(d.trabajador.getEmail(), () -> movimientoService.registrar(ajusteAlmacen(d, "1"))))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(autorizacionRepository.existsByTrabajadorIdAndAlmacenIdAndAccionAndEstado(d.trabajador.getId(), d.almacen.getId(), AccionAlmacen.AJUSTE, EstadoAutorizacionAlmacen.ACTIVA)).isFalse();
    }

    private Datos datosBase(BigDecimal cantidadInicial) {
        UnidadMedida unidad = unidad(prefix + "-unidad");
        Tienda a = tienda(prefix + "-a");
        Tienda b = tienda(prefix + "-b");
        Almacen almacen = new Almacen();
        almacen.setNombre(prefix + "-almacen");
        almacen = almacenRepository.saveAndFlush(almacen);
        Producto producto = new Producto();
        producto.setNombre(prefix + "-producto");
        producto.setUnidadBase(unidad);
        producto.setPrecioUnitario(BigDecimal.ONE);
        producto = productoRepository.saveAndFlush(producto);
        Usuario adminA = usuario(prefix + "-admin-a", Rol.ADMINISTRADOR, a);
        Usuario adminB = usuario(prefix + "-admin-b", Rol.ADMINISTRADOR, b);
        Usuario trabajador = usuario(prefix + "-trabajador", Rol.TRABAJADOR, a);
        Datos datos = new Datos(unidad, a, b, almacen, producto, adminA, adminB, trabajador);
        if (cantidadInicial != null) guardarStock(datos, a, cantidadInicial.toPlainString());
        return datos;
    }

    private UnidadMedida unidad(String nombre) {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre(nombre);
        return unidadRepository.saveAndFlush(unidad);
    }

    private Tienda tienda(String nombre) {
        Tienda tienda = new Tienda();
        tienda.setNombre(nombre);
        tienda.setRuc(UUID.randomUUID().toString());
        return tiendaRepository.saveAndFlush(tienda);
    }

    private Usuario usuario(String nombre, Rol rol, Tienda tienda) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(nombre + "@test.local");
        usuario.setPasswordHash("no-usar");
        usuario.setRol(rol);
        usuario.setTienda(tienda);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private void guardarStock(Datos d, Tienda tienda, String cantidad) {
        Stock stock = new Stock();
        stock.setProducto(d.producto);
        stock.setUbicacionTipo(UbicacionTipo.TIENDA);
        stock.setUbicacionId(tienda.getId());
        stock.setCantidad(new BigDecimal(cantidad));
        stockRepository.saveAndFlush(stock);
    }

    private Stock stock(Datos d, Tienda tienda) {
        return stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(d.producto.getId(), UbicacionTipo.TIENDA, tienda.getId()).orElse(null);
    }

    private Movimiento trasladoPendiente(Datos d, Tienda origen, Tienda destino, String cantidad) {
        Movimiento movimiento = new Movimiento();
        movimiento.setProducto(d.producto);
        movimiento.setUnidad(d.unidad);
        movimiento.setTipo(MovimientoTipo.TRASLADO);
        movimiento.setCantidad(new BigDecimal(cantidad));
        movimiento.setUbicacionOrigenTipo(UbicacionTipo.TIENDA);
        movimiento.setUbicacionOrigenId(origen.getId());
        movimiento.setUbicacionDestinoTipo(UbicacionTipo.TIENDA);
        movimiento.setUbicacionDestinoId(destino.getId());
        movimiento.setUsuario(d.trabajador);
        movimiento.setEstado(MovimientoEstado.PENDIENTE);
        movimiento.setMotivo(prefix + "-traslado");
        return movimientoRepository.saveAndFlush(movimiento);
    }

    private VentaRequest venta(Datos d, String cantidad) {
        return venta(d, cantidad, d.unidad.getId());
    }

    private VentaRequest venta(Datos d, String cantidad, Long unidadId) {
        VentaItemRequest item = new VentaItemRequest();
        item.setProductoId(d.producto.getId());
        item.setCantidad(new BigDecimal(cantidad));
        item.setUnidadId(unidadId);
        item.setPrecioUnitario(BigDecimal.ONE);
        VentaRequest venta = new VentaRequest();
        venta.setTiendaId(d.tiendaA.getId());
        venta.setItems(List.of(item));
        return venta;
    }

    private MovimientoRequest entrada(Datos d, String cantidad) {
        MovimientoRequest request = movimiento(d, MovimientoTipo.ENTRADA, cantidad, UbicacionTipo.TIENDA, d.tiendaA.getId());
        request.setMotivo(prefix + "-entrada");
        return request;
    }

    private MovimientoRequest ajusteAlmacen(Datos d, String cantidad) {
        MovimientoRequest request = movimiento(d, MovimientoTipo.AJUSTE, cantidad, UbicacionTipo.ALMACEN, d.almacen.getId());
        request.setMotivo(prefix + "-ajuste");
        return request;
    }

    private MovimientoRequest movimiento(Datos d, MovimientoTipo tipo, String cantidad, UbicacionTipo ubicacion, Long ubicacionId) {
        MovimientoRequest request = new MovimientoRequest();
        request.setProductoId(d.producto.getId());
        request.setUnidadId(d.unidad.getId());
        request.setTipo(tipo);
        request.setCantidad(new BigDecimal(cantidad));
        request.setUbicacionOrigenTipo(ubicacion);
        request.setUbicacionOrigenId(ubicacionId);
        return request;
    }

    private AutorizacionAlmacenRequest autorizacion(Datos d) {
        AutorizacionAlmacenRequest request = new AutorizacionAlmacenRequest();
        request.setTrabajadorId(d.trabajador.getId());
        request.setAlmacenId(d.almacen.getId());
        request.setAccion(AccionAlmacen.AJUSTE);
        return request;
    }

    private Resultado[] ejecutarEnParalelo(String emailA, Callable<?> accionA, String emailB, Callable<?> accionB) throws Exception {
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Resultado> a = executor.submit(actor(emailA, accionA, listos, iniciar));
            Future<Resultado> b = executor.submit(actor(emailB, accionB, listos, iniciar));
            assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            return new Resultado[]{a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)};
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private Callable<Resultado> actor(String email, Callable<?> accion, CountDownLatch listos, CountDownLatch iniciar) {
        return () -> {
            try {
                listos.countDown();
                if (!iniciar.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("No se inició la operación concurrente");
                return new Resultado(true, como(email, accion), null);
            } catch (Throwable error) {
                return new Resultado(false, null, error);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private <T> T como(String email, Callable<T> accion) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        try {
            return accion.call();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private int exitos(Resultado[] resultados) {
        return (int) java.util.Arrays.stream(resultados).filter(Resultado::exito).count();
    }

    @SafeVarargs
    private final boolean falloDeTipo(Resultado[] resultados, Class<? extends Throwable>... tipos) {
        return java.util.Arrays.stream(resultados)
                .map(Resultado::error)
                .filter(java.util.Objects::nonNull)
                .anyMatch(error -> java.util.Arrays.stream(tipos).anyMatch(tipo -> tipo.isInstance(error)));
    }

    private int contar(String sql, Object... argumentos) {
        return jdbc.queryForObject(sql, Integer.class, argumentos);
    }

    private record Datos(UnidadMedida unidad, Tienda tiendaA, Tienda tiendaB, Almacen almacen, Producto producto,
                         Usuario adminA, Usuario adminB, Usuario trabajador) { }

    private record Resultado(boolean exito, Object valor, Throwable error) { }
}
