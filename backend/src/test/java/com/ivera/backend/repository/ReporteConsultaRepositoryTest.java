package com.ivera.backend.repository;

import com.ivera.backend.entity.ConteoDetalle;
import com.ivera.backend.entity.ConteoInventario;
import com.ivera.backend.entity.Movimiento;
import com.ivera.backend.entity.Producto;
import com.ivera.backend.entity.UnidadMedida;
import com.ivera.backend.entity.Usuario;
import com.ivera.backend.enums.ConteoEstado;
import com.ivera.backend.enums.MovimientoEstado;
import com.ivera.backend.enums.MovimientoTipo;
import com.ivera.backend.enums.Rol;
import com.ivera.backend.enums.UbicacionTipo;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:reportes_consulta;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ReporteConsultaRepositoryTest {

    @Autowired private MovimientoRepository movimientoRepository;
    @Autowired private ConteoDetalleRepository conteoDetalleRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void buscarParaReporteAceptaFechasYTiendasNulas() {
        Datos datos = crearDatos();
        Movimiento antiguo = persistirMovimiento(datos, LocalDateTime.of(2026, 1, 1, 10, 0));
        Movimiento reciente = persistirMovimiento(datos, LocalDateTime.of(2026, 1, 2, 10, 0));
        entityManager.flush();
        entityManager.clear();

        var resultado = movimientoRepository.buscarParaReporte(null, UbicacionTipo.TIENDA, null, null);

        assertThat(resultado).extracting(Movimiento::getId).containsExactly(reciente.getId(), antiguo.getId());
    }

    @Test
    void buscarDiferenciasAceptaFechasNulasYFiltraCeros() {
        Datos datos = crearDatos();
        persistirDetalleConteo(datos, new BigDecimal("-2"), LocalDateTime.of(2026, 1, 2, 10, 0));
        persistirDetalleConteo(datos, BigDecimal.ZERO, LocalDateTime.of(2026, 1, 3, 10, 0));
        entityManager.flush();
        entityManager.clear();

        var resultado = conteoDetalleRepository.buscarDiferencias(null, null);

        assertThat(resultado).singleElement().satisfies(detalle ->
                assertThat(detalle.getDiferencia()).isEqualByComparingTo("-2")
        );
    }

    private Datos crearDatos() {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("unidad-" + System.nanoTime());
        entityManager.persist(unidad);

        Producto producto = new Producto();
        producto.setNombre("Perno");
        producto.setUnidadBase(unidad);
        producto.setPrecioUnitario(BigDecimal.ONE);
        entityManager.persist(producto);

        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setEmail("ana-" + System.nanoTime() + "@example.test");
        usuario.setPasswordHash("hash");
        usuario.setRol(Rol.ADMINISTRADOR);
        entityManager.persist(usuario);
        return new Datos(unidad, producto, usuario);
    }

    private Movimiento persistirMovimiento(Datos datos, LocalDateTime fecha) {
        Movimiento movimiento = new Movimiento();
        movimiento.setProducto(datos.producto());
        movimiento.setTipo(MovimientoTipo.ENTRADA);
        movimiento.setCantidad(BigDecimal.ONE);
        movimiento.setUnidad(datos.unidad());
        movimiento.setUbicacionOrigenTipo(UbicacionTipo.TIENDA);
        movimiento.setUbicacionOrigenId(1L);
        movimiento.setUsuario(datos.usuario());
        movimiento.setEstado(MovimientoEstado.APROBADO);
        movimiento.setFecha(fecha);
        entityManager.persist(movimiento);
        return movimiento;
    }

    private void persistirDetalleConteo(Datos datos, BigDecimal diferencia, LocalDateTime fecha) {
        ConteoInventario conteo = new ConteoInventario();
        conteo.setUbicacionTipo(UbicacionTipo.TIENDA);
        conteo.setUbicacionId(1L);
        conteo.setUsuario(datos.usuario());
        conteo.setEstado(ConteoEstado.FINALIZADO);
        conteo.setFecha(fecha);
        entityManager.persist(conteo);

        ConteoDetalle detalle = new ConteoDetalle();
        detalle.setConteo(conteo);
        detalle.setProducto(datos.producto());
        detalle.setCantidadContada(new BigDecimal("8"));
        detalle.setCantidadSistema(new BigDecimal("10"));
        detalle.setDiferencia(diferencia);
        entityManager.persist(detalle);
    }

    private record Datos(UnidadMedida unidad, Producto producto, Usuario usuario) { }
}
