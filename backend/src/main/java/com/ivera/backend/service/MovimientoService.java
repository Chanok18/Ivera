package com.ivera.backend.service;

import com.ivera.backend.dto.MovimientoRequest;
import com.ivera.backend.dto.MovimientoResponse;
import com.ivera.backend.dto.UnidadResponse;
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
import java.util.List;

@Service
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoPresentacionRepository presentacionRepository;
    private final TiendaRepository tiendaRepository;
    private final AlmacenRepository almacenRepository;
    private final StockRepository stockRepository;
    private final UsuarioRepository usuarioRepository;

    public MovimientoService(MovimientoRepository movimientoRepository,
                             ProductoRepository productoRepository,
                             UnidadMedidaRepository unidadMedidaRepository,
                             ProductoPresentacionRepository presentacionRepository,
                             TiendaRepository tiendaRepository,
                             AlmacenRepository almacenRepository,
                             StockRepository stockRepository,
                             UsuarioRepository usuarioRepository) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.presentacionRepository = presentacionRepository;
        this.tiendaRepository = tiendaRepository;
        this.almacenRepository = almacenRepository;
        this.stockRepository = stockRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public MovimientoResponse registrar(MovimientoRequest request) {
        if (request.getCantidad() == null || request.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + request.getProductoId()));

        UnidadMedida unidad = unidadMedidaRepository.findById(request.getUnidadId())
                .orElseThrow(() -> new EntityNotFoundException("Unidad de medida no encontrada: " + request.getUnidadId()));

        // Validar ubicación de origen
        validarUbicacion(request.getUbicacionOrigenTipo(), request.getUbicacionOrigenId());

        // Si es TRASLADO, validar ubicación de destino
        if (request.getTipo() == MovimientoTipo.TRASLADO) {
            if (request.getUbicacionDestinoTipo() == null || request.getUbicacionDestinoId() == null) {
                throw new IllegalArgumentException("El traslado requiere ubicación de destino (tipo e ID)");
            }
            validarUbicacion(request.getUbicacionDestinoTipo(), request.getUbicacionDestinoId());
            if (request.getUbicacionOrigenTipo() == request.getUbicacionDestinoTipo() &&
                    request.getUbicacionOrigenId().equals(request.getUbicacionDestinoId())) {
                throw new IllegalArgumentException("La ubicación de origen y destino no pueden ser la misma en un traslado");
            }
        }

        // Calcular cantidad en unidad base
        BigDecimal cantidadBase = calcularCantidadBase(producto, unidad, request.getCantidad());

        // Obtener usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado: " + email));

        Movimiento movimiento = new Movimiento();
        movimiento.setProducto(producto);
        movimiento.setTipo(request.getTipo());
        movimiento.setCantidad(cantidadBase);
        movimiento.setUnidad(unidad);
        movimiento.setUbicacionOrigenTipo(request.getUbicacionOrigenTipo());
        movimiento.setUbicacionOrigenId(request.getUbicacionOrigenId());
        movimiento.setUbicacionDestinoTipo(request.getUbicacionDestinoTipo());
        movimiento.setUbicacionDestinoId(request.getUbicacionDestinoId());
        movimiento.setUsuario(usuario);
        movimiento.setMotivo(request.getMotivo());

        // ENTRADA y SALIDA actualizan stock inmediatamente (estado APROBADO automático)
        // AJUSTE y TRASLADO quedan en estado PENDIENTE sin tocar stock
        if (request.getTipo() == MovimientoTipo.ENTRADA || request.getTipo() == MovimientoTipo.SALIDA) {
            movimiento.setEstado(MovimientoEstado.APROBADO);
            movimiento.setAprobadoPor(usuario);

            Stock stock = stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(
                    producto.getId(), request.getUbicacionOrigenTipo(), request.getUbicacionOrigenId()
            ).orElseGet(() -> {
                Stock nuevo = new Stock();
                nuevo.setProducto(producto);
                nuevo.setUbicacionTipo(request.getUbicacionOrigenTipo());
                nuevo.setUbicacionId(request.getUbicacionOrigenId());
                nuevo.setCantidad(BigDecimal.ZERO);
                return nuevo;
            });

            if (request.getTipo() == MovimientoTipo.ENTRADA) {
                stock.setCantidad(stock.getCantidad().add(cantidadBase));
            } else {
                if (stock.getCantidad().compareTo(cantidadBase) < 0) {
                    throw new IllegalArgumentException("Stock insuficiente en la ubicación de origen para realizar la salida");
                }
                stock.setCantidad(stock.getCantidad().subtract(cantidadBase));
            }
            stockRepository.save(stock);
        } else {
            movimiento.setEstado(MovimientoEstado.PENDIENTE);
        }

        movimiento = movimientoRepository.save(movimiento);
        return toResponse(movimiento);
    }

    private void validarUbicacion(UbicacionTipo tipo, Long id) {
        if (tipo == UbicacionTipo.TIENDA) {
            if (!tiendaRepository.existsById(id)) {
                throw new EntityNotFoundException("Tienda no encontrada con ID: " + id);
            }
        } else if (tipo == UbicacionTipo.ALMACEN) {
            if (!almacenRepository.existsById(id)) {
                throw new EntityNotFoundException("Almacén no encontrado con ID: " + id);
            }
        } else {
            throw new IllegalArgumentException("Tipo de ubicación inválido: " + tipo);
        }
    }

    private String obtenerNombreUbicacion(UbicacionTipo tipo, Long id) {
        if (tipo == UbicacionTipo.TIENDA) {
            return tiendaRepository.findById(id).map(Tienda::getNombre).orElse("Desconocida");
        } else if (tipo == UbicacionTipo.ALMACEN) {
            return almacenRepository.findById(id).map(Almacen::getNombre).orElse("Desconocida");
        }
        return null;
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

    private MovimientoResponse toResponse(Movimiento m) {
        MovimientoResponse res = new MovimientoResponse();
        res.setId(m.getId());
        res.setProductoId(m.getProducto().getId());
        res.setProductoNombre(m.getProducto().getNombre());
        res.setTipo(m.getTipo());
        res.setCantidad(m.getCantidad());
        res.setUnidad(new UnidadResponse(m.getUnidad().getId(), m.getUnidad().getNombre()));
        res.setUbicacionOrigenTipo(m.getUbicacionOrigenTipo());
        res.setUbicacionOrigenId(m.getUbicacionOrigenId());
        res.setUbicacionOrigenNombre(obtenerNombreUbicacion(m.getUbicacionOrigenTipo(), m.getUbicacionOrigenId()));
        res.setUbicacionDestinoTipo(m.getUbicacionDestinoTipo());
        res.setUbicacionDestinoId(m.getUbicacionDestinoId());
        res.setUbicacionDestinoNombre(m.getUbicacionDestinoId() != null ? obtenerNombreUbicacion(m.getUbicacionDestinoTipo(), m.getUbicacionDestinoId()) : null);
        res.setUsuarioId(m.getUsuario().getId());
        res.setUsuarioNombre(m.getUsuario().getNombre());
        res.setEstado(m.getEstado());
        res.setAprobadoPorId(m.getAprobadoPor() != null ? m.getAprobadoPor().getId() : null);
        res.setAprobadoPorNombre(m.getAprobadoPor() != null ? m.getAprobadoPor().getNombre() : null);
        res.setFecha(m.getFecha());
        res.setMotivo(m.getMotivo());
        return res;
    }
}
