package com.ivera.backend.repository;

import com.ivera.backend.entity.VentaDetalle;
import com.ivera.backend.dto.ProductoMasVendidoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {
    List<VentaDetalle> findByVentaId(Long ventaId);
    boolean existsByCantidadBaseIsNull();
    boolean existsByCantidadBaseIsNullAndVentaTiendaId(Long tiendaId);

    @Query("""
            select new com.ivera.backend.dto.ProductoMasVendidoResponse(
                vd.producto.id, vd.producto.nombre, sum(vd.cantidadBase)
            )
            from VentaDetalle vd
            group by vd.producto.id, vd.producto.nombre
            order by sum(vd.cantidadBase) desc
            """)
    List<ProductoMasVendidoResponse> buscarProductosMasVendidos(Pageable pageable);

    @Query("""
            select new com.ivera.backend.dto.ProductoMasVendidoResponse(
                vd.producto.id, vd.producto.nombre, sum(vd.cantidadBase)
            )
            from VentaDetalle vd
            where vd.venta.tienda.id = :tiendaId
            group by vd.producto.id, vd.producto.nombre
            order by sum(vd.cantidadBase) desc
            """)
    List<ProductoMasVendidoResponse> buscarProductosMasVendidosPorTienda(Long tiendaId, Pageable pageable);
}
