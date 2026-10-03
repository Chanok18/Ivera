package com.ivera.backend.service;

import com.ivera.backend.dto.*;
import com.ivera.backend.entity.*;
import com.ivera.backend.enums.MovimientoEstado;
import com.ivera.backend.enums.MovimientoTipo;
import com.ivera.backend.enums.UbicacionTipo;
import com.ivera.backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CompraService {

    private final CompraRepository compraRepository;
    private final CompraDetalleRepository compraDetalleRepository;
    private final ProveedorRepository proveedorRepository;
    private final TiendaRepository tiendaRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoPresentacionRepository presentacionRepository;
    private final StockRepository stockRepository;
    private final MovimientoRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;

    public CompraService(CompraRepository compraRepository,
                         CompraDetalleRepository compraDetalleRepository,
                         ProveedorRepository proveedorRepository,
                         TiendaRepository tiendaRepository,
                         ProductoRepository productoRepository,
                         UnidadMedidaRepository unidadMedidaRepository,
                         ProductoPresentacionRepository presentacionRepository,
                         StockRepository stockRepository,
                         MovimientoRepository movimientoRepository,
                         UsuarioRepository usuarioRepository) {
        this.compraRepository = compraRepository;
        this.compraDetalleRepository = compraDetalleRepository;
        this.proveedorRepository = proveedorRepository;
        this.tiendaRepository = tiendaRepository;
        this.productoRepository = productoRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.presentacionRepository = presentacionRepository;
        this.stockRepository = stockRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public CompraResponse registrar(CompraRequest request) {
        Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + request.getProveedorId()));

        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new EntityNotFoundException("Tienda no encontrada: " + request.getTiendaId()));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado: " + email));

        // Calcular total antes de guardar la compra y resolver productos previamente
        BigDecimal total = BigDecimal.ZERO;
        List<Producto> productosResueltos = new ArrayList<>();

        for (CompraItemRequest itemReq : request.getItems()) {
            Producto producto = resolverProducto(itemReq);
            productosResueltos.add(producto);

            BigDecimal subtotal = itemReq.getCantidad().multiply(itemReq.getPrecioUnitario());
            total = total.add(subtotal);
        }

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setTienda(tienda);
        compra.setUsuario(usuario);
        compra.setTotal(total);
        compra.setEstado("RECIBIDA");
        compra = compraRepository.save(compra);

        List<CompraDetalle> detalles = new ArrayList<>();

        for (int i = 0; i < request.getItems().size(); i++) {
            CompraItemRequest itemReq = request.getItems().get(i);
            Producto producto = productosResueltos.get(i);

            UnidadMedida unidad = unidadMedidaRepository.findById(itemReq.getUnidadId())
                    .orElseThrow(() -> new EntityNotFoundException("Unidad de medida no encontrada: " + itemReq.getUnidadId()));

            CompraDetalle detalle = new CompraDetalle();
            detalle.setCompra(compra);
            detalle.setProducto(producto);
            detalle.setCantidad(itemReq.getCantidad());
            detalle.setUnidad(unidad);
            detalle.setPrecioUnitario(itemReq.getPrecioUnitario());
            detalles.add(compraDetalleRepository.save(detalle));

            // Calcular cantidad en unidad base
            BigDecimal cantidadBase = calcularCantidadBase(producto, unidad, itemReq.getCantidad());

            // Crear movimiento de ENTRADA (aprobado automático)
            Movimiento movimiento = new Movimiento();
            movimiento.setProducto(producto);
            movimiento.setTipo(MovimientoTipo.ENTRADA);
            movimiento.setCantidad(cantidadBase);
            movimiento.setUnidad(unidad);
            movimiento.setUbicacionOrigenTipo(UbicacionTipo.TIENDA);
            movimiento.setUbicacionOrigenId(tienda.getId());
            movimiento.setUsuario(usuario);
            movimiento.setEstado(MovimientoEstado.APROBADO);
            movimiento.setAprobadoPor(usuario);
            movimiento.setMotivo("Compra #" + compra.getId() + " - Proveedor: " + proveedor.getNombre());
            movimientoRepository.save(movimiento);

            // Sumar al stock de la tienda
            Stock stock = stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(
                    producto.getId(), UbicacionTipo.TIENDA, tienda.getId()
            ).orElse(null);

            if (stock == null) {
                stock = new Stock();
                stock.setProducto(producto);
                stock.setUbicacionTipo(UbicacionTipo.TIENDA);
                stock.setUbicacionId(tienda.getId());
                stock.setCantidad(BigDecimal.ZERO);
            }
            stock.setCantidad(stock.getCantidad().add(cantidadBase));
            stockRepository.save(stock);
        }

        return toResponse(compra, detalles);
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listar(Long tiendaId) {
        List<Compra> compras = (tiendaId != null) ? compraRepository.findByTiendaId(tiendaId) : compraRepository.findAll();
        return compras.stream().map(c -> toResponse(c, compraDetalleRepository.findByCompraId(c.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public CompraResponse obtenerPorId(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Compra no encontrada: " + id));
        return toResponse(compra, compraDetalleRepository.findByCompraId(compra.getId()));
    }

    private Producto resolverProducto(CompraItemRequest itemReq) {
        if (itemReq.getProductoId() != null) {
            return productoRepository.findById(itemReq.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto con ID " + itemReq.getProductoId() + " no encontrado"));
        }
        if (itemReq.getCodigoBarras() != null && !itemReq.getCodigoBarras().isBlank()) {
            return productoRepository.findByCodigoBarras(itemReq.getCodigoBarras())
                    .orElseThrow(() -> new EntityNotFoundException("Producto con código de barras " + itemReq.getCodigoBarras() + " no encontrado"));
        }
        throw new IllegalArgumentException("Debe proporcionar productoId o codigoBarras para cada ítem de compra");
    }

    private BigDecimal calcularCantidadBase(Producto producto, UnidadMedida unidad, BigDecimal cantidadInput) {
        if (producto.getUnidadBase().getId().equals(unidad.getId())) {
            return cantidadInput;
        }
        List<ProductoPresentacion> presentaciones = presentacionRepository.findByProductoId(producto.getId());
        for (ProductoPresentacion pp : presentaciones) {
            if (pp.getUnidad().getId().equals(unidad.getId())) {
                return cantidadInput.multiply(BigDecimal.valueOf(pp.getFactorConversion()));
            }
        }
        throw new IllegalArgumentException("La unidad seleccionada no corresponde a la unidad base ni a ninguna presentación válida del producto");
    }

    private CompraResponse toResponse(Compra c, List<CompraDetalle> detalles) {
        CompraResponse res = new CompraResponse();
        res.setId(c.getId());
        res.setProveedorId(c.getProveedor().getId());
        res.setProveedorNombre(c.getProveedor().getNombre());
        res.setTiendaId(c.getTienda().getId());
        res.setTiendaNombre(c.getTienda().getNombre());
        res.setUsuarioId(c.getUsuario().getId());
        res.setUsuarioNombre(c.getUsuario().getNombre());
        res.setFecha(c.getFecha());
        res.setTotal(c.getTotal());
        res.setEstado(c.getEstado());

        List<CompraDetalleResponse> itemResponses = detalles.stream().map(d -> {
            CompraDetalleResponse dr = new CompraDetalleResponse();
            dr.setId(d.getId());
            dr.setProductoId(d.getProducto().getId());
            dr.setProductoNombre(d.getProducto().getNombre());
            dr.setCantidad(d.getCantidad());
            dr.setUnidad(new UnidadResponse(d.getUnidad().getId(), d.getUnidad().getNombre()));
            dr.setPrecioUnitario(d.getPrecioUnitario());
            return dr;
        }).toList();

        res.setItems(itemResponses);
        return res;
    }
}
