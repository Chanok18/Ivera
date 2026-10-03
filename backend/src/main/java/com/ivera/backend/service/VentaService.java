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
public class VentaService {

    private final VentaRepository ventaRepository;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final TiendaRepository tiendaRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoPresentacionRepository presentacionRepository;
    private final StockRepository stockRepository;
    private final MovimientoRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;

    public VentaService(VentaRepository ventaRepository,
                        VentaDetalleRepository ventaDetalleRepository,
                        TiendaRepository tiendaRepository,
                        ProductoRepository productoRepository,
                        UnidadMedidaRepository unidadMedidaRepository,
                        ProductoPresentacionRepository presentacionRepository,
                        StockRepository stockRepository,
                        MovimientoRepository movimientoRepository,
                        UsuarioRepository usuarioRepository) {
        this.ventaRepository = ventaRepository;
        this.ventaDetalleRepository = ventaDetalleRepository;
        this.tiendaRepository = tiendaRepository;
        this.productoRepository = productoRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.presentacionRepository = presentacionRepository;
        this.stockRepository = stockRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public VentaResponse registrar(VentaRequest request) {
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new EntityNotFoundException("Tienda no encontrada: " + request.getTiendaId()));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado: " + email));

        // 1. Resolver productos y verificación previa de stock para todos los ítems (Todo o Nada)
        BigDecimal total = BigDecimal.ZERO;
        List<Producto> productosResueltos = new ArrayList<>();

        for (VentaItemRequest itemReq : request.getItems()) {
            Producto producto = resolverProducto(itemReq);
            productosResueltos.add(producto);

            UnidadMedida unidad = unidadMedidaRepository.findById(itemReq.getUnidadId())
                    .orElseThrow(() -> new EntityNotFoundException("Unidad de medida no encontrada: " + itemReq.getUnidadId()));

            BigDecimal cantidadBase = calcularCantidadBase(producto, unidad, itemReq.getCantidad());

            Stock stock = stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(
                    producto.getId(), UbicacionTipo.TIENDA, tienda.getId()
            ).orElse(null);

            BigDecimal stockDisponible = stock != null ? stock.getCantidad() : BigDecimal.ZERO;

            if (stockDisponible.compareTo(cantidadBase) < 0) {
                throw new IllegalArgumentException("Stock insuficiente para el producto: " + producto.getNombre() +
                        " (Disponible: " + stockDisponible + ", Requerido: " + cantidadBase + ")");
            }

            BigDecimal subtotal = itemReq.getCantidad().multiply(itemReq.getPrecioUnitario());
            total = total.add(subtotal);
        }

        // 2. Procesar la venta
        Venta venta = new Venta();
        venta.setTienda(tienda);
        venta.setUsuario(usuario);
        venta.setTotal(total);
        venta = ventaRepository.save(venta);

        List<VentaDetalle> detalles = new ArrayList<>();

        for (int i = 0; i < request.getItems().size(); i++) {
            VentaItemRequest itemReq = request.getItems().get(i);
            Producto producto = productosResueltos.get(i);
            UnidadMedida unidad = unidadMedidaRepository.findById(itemReq.getUnidadId()).get();

            VentaDetalle detalle = new VentaDetalle();
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setCantidad(itemReq.getCantidad());
            detalle.setUnidad(unidad);
            detalle.setPrecioUnitario(itemReq.getPrecioUnitario());
            detalles.add(ventaDetalleRepository.save(detalle));

            BigDecimal cantidadBase = calcularCantidadBase(producto, unidad, itemReq.getCantidad());

            // Crear movimiento de SALIDA (aprobado automático)
            Movimiento movimiento = new Movimiento();
            movimiento.setProducto(producto);
            movimiento.setTipo(MovimientoTipo.SALIDA);
            movimiento.setCantidad(cantidadBase);
            movimiento.setUnidad(unidad);
            movimiento.setUbicacionOrigenTipo(UbicacionTipo.TIENDA);
            movimiento.setUbicacionOrigenId(tienda.getId());
            movimiento.setUsuario(usuario);
            movimiento.setEstado(MovimientoEstado.APROBADO);
            movimiento.setAprobadoPor(usuario);
            movimiento.setMotivo("Venta #" + venta.getId());
            movimientoRepository.save(movimiento);

            // Descontar del stock de la tienda
            Stock stock = stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(
                    producto.getId(), UbicacionTipo.TIENDA, tienda.getId()
            ).get();
            stock.setCantidad(stock.getCantidad().subtract(cantidadBase));
            stockRepository.save(stock);
        }

        return toResponse(venta, detalles);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listar(Long tiendaId) {
        List<Venta> ventas = (tiendaId != null) ? ventaRepository.findByTiendaId(tiendaId) : ventaRepository.findAll();
        return ventas.stream().map(v -> toResponse(v, ventaDetalleRepository.findByVentaId(v.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public VentaResponse obtenerPorId(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + id));
        return toResponse(venta, ventaDetalleRepository.findByVentaId(venta.getId()));
    }

    private Producto resolverProducto(VentaItemRequest itemReq) {
        if (itemReq.getProductoId() != null) {
            return productoRepository.findById(itemReq.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto con ID " + itemReq.getProductoId() + " no encontrado"));
        }
        if (itemReq.getCodigoBarras() != null && !itemReq.getCodigoBarras().isBlank()) {
            return productoRepository.findByCodigoBarras(itemReq.getCodigoBarras())
                    .orElseThrow(() -> new EntityNotFoundException("Producto con código de barras " + itemReq.getCodigoBarras() + " no encontrado"));
        }
        throw new IllegalArgumentException("Debe proporcionar productoId o codigoBarras para cada ítem de venta");
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

    private VentaResponse toResponse(Venta v, List<VentaDetalle> detalles) {
        VentaResponse res = new VentaResponse();
        res.setId(v.getId());
        res.setTiendaId(v.getTienda().getId());
        res.setTiendaNombre(v.getTienda().getNombre());
        res.setUsuarioId(v.getUsuario().getId());
        res.setUsuarioNombre(v.getUsuario().getNombre());
        res.setFecha(v.getFecha());
        res.setTotal(v.getTotal());

        List<VentaDetalleResponse> itemResponses = detalles.stream().map(d -> {
            VentaDetalleResponse dr = new VentaDetalleResponse();
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
