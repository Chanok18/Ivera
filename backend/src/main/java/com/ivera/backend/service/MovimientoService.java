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
        if (request.getTipo() == MovimientoTipo.AJUSTE) {
            if (request.getCantidad() == null || request.getCantidad().compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("La cantidad del ajuste no puede ser cero");
            }
        } else {
            if (request.getCantidad() == null || request.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
            }
        }

        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + request.getProductoId()));

        UnidadMedida unidad = unidadMedidaRepository.findById(request.getUnidadId())
                .orElseThrow(() -> new EntityNotFoundException("Unidad de medida no encontrada: " + request.getUnidadId()));

        validarUbicacion(request.getUbicacionOrigenTipo(), request.getUbicacionOrigenId());

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

        BigDecimal cantidadBase = calcularCantidadBase(producto, unidad, request.getCantidad());

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

        if (request.getTipo() == MovimientoTipo.ENTRADA || request.getTipo() == MovimientoTipo.SALIDA) {
            movimiento.setEstado(MovimientoEstado.APROBADO);
            movimiento.setAprobadoPor(usuario);

            Stock stock = stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(
                    producto.getId(), request.getUbicacionOrigenTipo(), request.getUbicacionOrigenId()
            ).orElse(null);

            if (stock == null) {
                stock = new Stock();
                stock.setProducto(producto);
                stock.setUbicacionTipo(request.getUbicacionOrigenTipo());
                stock.setUbicacionId(request.getUbicacionOrigenId());
                stock.setCantidad(BigDecimal.ZERO);
            }

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

    @Transactional
    public MovimientoResponse aprobar(Long id) {
        Movimiento movimiento = movimientoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movimiento no encontrado: " + id));

        if (movimiento.getEstado() != MovimientoEstado.PENDIENTE) {
            throw new IllegalStateException("El movimiento no se encuentra en estado PENDIENTE (estado actual: " + movimiento.getEstado() + "). No se puede aprobar.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Usuario admin = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario administrador no encontrado: " + email));

        movimiento.setEstado(MovimientoEstado.APROBADO);
        movimiento.setAprobadoPor(admin);

        if (movimiento.getTipo() == MovimientoTipo.TRASLADO) {
            Stock stockOrigen = stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(
                    movimiento.getProducto().getId(), movimiento.getUbicacionOrigenTipo(), movimiento.getUbicacionOrigenId()
            ).orElse(null);

            if (stockOrigen == null) {
                stockOrigen = new Stock();
                stockOrigen.setProducto(movimiento.getProducto());
                stockOrigen.setUbicacionTipo(movimiento.getUbicacionOrigenTipo());
                stockOrigen.setUbicacionId(movimiento.getUbicacionOrigenId());
                stockOrigen.setCantidad(BigDecimal.ZERO);
            }

            if (stockOrigen.getCantidad().compareTo(movimiento.getCantidad()) < 0) {
                throw new IllegalStateException("Stock insuficiente en la ubicación de origen para aprobar el traslado");
            }
            stockOrigen.setCantidad(stockOrigen.getCantidad().subtract(movimiento.getCantidad()));
            stockRepository.save(stockOrigen);

            Stock stockDestino = stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(
                    movimiento.getProducto().getId(), movimiento.getUbicacionDestinoTipo(), movimiento.getUbicacionDestinoId()
            ).orElse(null);

            if (stockDestino == null) {
                stockDestino = new Stock();
                stockDestino.setProducto(movimiento.getProducto());
                stockDestino.setUbicacionTipo(movimiento.getUbicacionDestinoTipo());
                stockDestino.setUbicacionId(movimiento.getUbicacionDestinoId());
                stockDestino.setCantidad(BigDecimal.ZERO);
            }

            stockDestino.setCantidad(stockDestino.getCantidad().add(movimiento.getCantidad()));
            stockRepository.save(stockDestino);

        } else if (movimiento.getTipo() == MovimientoTipo.AJUSTE) {
            Stock stock = stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(
                    movimiento.getProducto().getId(), movimiento.getUbicacionOrigenTipo(), movimiento.getUbicacionOrigenId()
            ).orElse(null);

            if (stock == null) {
                stock = new Stock();
                stock.setProducto(movimiento.getProducto());
                stock.setUbicacionTipo(movimiento.getUbicacionOrigenTipo());
                stock.setUbicacionId(movimiento.getUbicacionOrigenId());
                stock.setCantidad(BigDecimal.ZERO);
            }

            BigDecimal nuevaCantidad = stock.getCantidad().add(movimiento.getCantidad());
            if (nuevaCantidad.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("El ajuste dejaría el stock en negativo, lo cual no está permitido");
            }
            stock.setCantidad(nuevaCantidad);
            stockRepository.save(stock);
        }

        movimiento = movimientoRepository.save(movimiento);
        return toResponse(movimiento);
    }

    @Transactional
    public MovimientoResponse rechazar(Long id) {
        Movimiento movimiento = movimientoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movimiento no encontrado: " + id));

        if (movimiento.getEstado() != MovimientoEstado.PENDIENTE) {
            throw new IllegalStateException("El movimiento no se encuentra en estado PENDIENTE (estado actual: " + movimiento.getEstado() + "). No se puede rechazar.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Usuario admin = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario administrador no encontrado: " + email));

        movimiento.setEstado(MovimientoEstado.RECHAZADO);
        movimiento.setAprobadoPor(admin);

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
