package com.ivera.backend.repository;

import com.ivera.backend.entity.*;
import com.ivera.backend.enums.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ventas;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class VentaDetalleRepositoryTest {

    @Autowired private VentaDetalleRepository ventaDetalleRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void agregaCantidadBaseYDetectaHistoricosSinSnapshot() {
        UnidadMedida unidad = persistirUnidad();
        Producto producto = persistirProducto(unidad);
        Venta venta = persistirVenta();
        persistirDetalle(venta, producto, unidad, "5", "5");
        persistirDetalle(venta, producto, unidad, "2", "24");
        entityManager.flush();
        entityManager.clear();

        var resultado = ventaDetalleRepository.buscarProductosMasVendidos(PageRequest.of(0, 5));

        assertThat(resultado).singleElement().satisfies(respuesta ->
                assertThat(respuesta.getCantidadTotalVendida()).isEqualByComparingTo("29")
        );
        assertThat(ventaDetalleRepository.existsByCantidadBaseIsNull()).isFalse();

        persistirDetalle(venta, producto, unidad, "1", null);
        entityManager.flush();

        assertThat(ventaDetalleRepository.existsByCantidadBaseIsNull()).isTrue();
    }

    private UnidadMedida persistirUnidad() {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("unidad");
        entityManager.persist(unidad);
        return unidad;
    }

    private Producto persistirProducto(UnidadMedida unidad) {
        Producto producto = new Producto();
        producto.setNombre("Tornillo");
        producto.setUnidadBase(unidad);
        producto.setPrecioUnitario(BigDecimal.ONE);
        entityManager.persist(producto);
        return producto;
    }

    private Venta persistirVenta() {
        Tienda tienda = new Tienda();
        tienda.setNombre("Romel");
        tienda.setRuc("20123456789");
        entityManager.persist(tienda);
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setEmail("ana@ivera.pe");
        usuario.setPasswordHash("hash");
        usuario.setRol(Rol.ADMINISTRADOR);
        entityManager.persist(usuario);
        Venta venta = new Venta();
        venta.setTienda(tienda);
        venta.setUsuario(usuario);
        venta.setTotal(BigDecimal.TEN);
        entityManager.persist(venta);
        return venta;
    }

    private void persistirDetalle(Venta venta, Producto producto, UnidadMedida unidad, String cantidad, String cantidadBase) {
        VentaDetalle detalle = new VentaDetalle();
        detalle.setVenta(venta);
        detalle.setProducto(producto);
        detalle.setUnidad(unidad);
        detalle.setCantidad(new BigDecimal(cantidad));
        detalle.setCantidadBase(cantidadBase != null ? new BigDecimal(cantidadBase) : null);
        detalle.setPrecioUnitario(BigDecimal.ONE);
        entityManager.persist(detalle);
    }
}
