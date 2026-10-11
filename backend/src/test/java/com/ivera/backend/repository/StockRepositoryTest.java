package com.ivera.backend.repository;

import com.ivera.backend.entity.Producto;
import com.ivera.backend.entity.Stock;
import com.ivera.backend.entity.UnidadMedida;
import com.ivera.backend.enums.UbicacionTipo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:reportes;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class StockRepositoryTest {

    @Autowired private StockRepository stockRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void buscarStockBajoSumaLasExistenciasDeTodasLasUbicaciones() {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("unidad");
        entityManager.persist(unidad);

        Producto producto = new Producto();
        producto.setNombre("Tornillo");
        producto.setUnidadBase(unidad);
        producto.setPrecioUnitario(BigDecimal.ONE);
        entityManager.persist(producto);

        persistirStock(producto, UbicacionTipo.TIENDA, 1L, "2");
        persistirStock(producto, UbicacionTipo.TIENDA, 2L, "3");
        persistirStock(producto, UbicacionTipo.ALMACEN, 1L, "4");
        entityManager.flush();
        entityManager.clear();

        var resultado = stockRepository.buscarStockBajo(new BigDecimal("9"));

        assertThat(resultado).singleElement().satisfies(respuesta ->
                assertThat(respuesta.getStockTotal()).isEqualByComparingTo("9")
        );
    }

    private void persistirStock(Producto producto, UbicacionTipo tipo, Long ubicacionId, String cantidad) {
        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setUbicacionTipo(tipo);
        stock.setUbicacionId(ubicacionId);
        stock.setCantidad(new BigDecimal(cantidad));
        entityManager.persist(stock);
    }
}
