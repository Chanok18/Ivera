package com.ivera.backend.service;

import com.ivera.backend.dto.*;
import com.ivera.backend.entity.*;
import com.ivera.backend.enums.ConteoEstado;
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
public class ConteoService {

    private final ConteoInventarioRepository conteoRepository;
    private final ConteoDetalleRepository detalleRepository;
    private final ProductoRepository productoRepository;
    private final TiendaRepository tiendaRepository;
    private final AlmacenRepository almacenRepository;
    private final StockRepository stockRepository;
    private final UsuarioRepository usuarioRepository;

    public ConteoService(ConteoInventarioRepository conteoRepository,
                         ConteoDetalleRepository detalleRepository,
                         ProductoRepository productoRepository,
                         TiendaRepository tiendaRepository,
                         AlmacenRepository almacenRepository,
                         StockRepository stockRepository,
                         UsuarioRepository usuarioRepository) {
        this.conteoRepository = conteoRepository;
        this.detalleRepository = detalleRepository;
        this.productoRepository = productoRepository;
        this.tiendaRepository = tiendaRepository;
        this.almacenRepository = almacenRepository;
        this.stockRepository = stockRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ConteoResponse iniciar(ConteoRequest request) {
        validarUbicacion(request.getUbicacionTipo(), request.getUbicacionId());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado: " + email));

        ConteoInventario conteo = new ConteoInventario();
        conteo.setUbicacionTipo(request.getUbicacionTipo());
        conteo.setUbicacionId(request.getUbicacionId());
        conteo.setUsuario(usuario);
        conteo.setEstado(ConteoEstado.EN_PROCESO);

        conteo = conteoRepository.save(conteo);
        return toResponse(conteo);
    }

    @Transactional
    public ConteoResponse agregarDetalle(Long conteoId, ConteoDetalleRequest request) {
        ConteoInventario conteo = conteoRepository.findById(conteoId)
                .orElseThrow(() -> new EntityNotFoundException("Conteo no encontrado: " + conteoId));

        if (conteo.getEstado() != ConteoEstado.EN_PROCESO) {
            throw new IllegalStateException("No se puede modificar un conteo que ya está finalizado");
        }

        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + request.getProductoId()));

        // Obtener cantidad en sistema para esa ubicación y producto
        Stock stock = stockRepository.findByProductoIdAndUbicacionTipoAndUbicacionId(
                producto.getId(), conteo.getUbicacionTipo(), conteo.getUbicacionId()
        ).orElse(null);

        BigDecimal cantidadSistema = stock != null ? stock.getCantidad() : BigDecimal.ZERO;
        BigDecimal cantidadContada = request.getCantidadContada();
        BigDecimal diferencia = cantidadContada.subtract(cantidadSistema);

        // Buscar si ya existe detalle para este producto en este conteo
        ConteoDetalle detalle = detalleRepository.findByConteoIdAndProductoId(conteo.getId(), producto.getId())
                .orElse(null);

        if (detalle == null) {
            detalle = new ConteoDetalle();
            detalle.setConteo(conteo);
            detalle.setProducto(producto);
        }

        detalle.setCantidadContada(cantidadContada);
        detalle.setCantidadSistema(cantidadSistema);
        detalle.setDiferencia(diferencia);

        detalleRepository.save(detalle);

        return toResponse(conteo);
    }

    @Transactional
    public ConteoResponse finalizar(Long conteoId) {
        ConteoInventario conteo = conteoRepository.findById(conteoId)
                .orElseThrow(() -> new EntityNotFoundException("Conteo no encontrado: " + conteoId));

        if (conteo.getEstado() != ConteoEstado.EN_PROCESO) {
            throw new IllegalStateException("El conteo ya se encuentra finalizado");
        }

        conteo.setEstado(ConteoEstado.FINALIZADO);
        conteo = conteoRepository.save(conteo);
        return toResponse(conteo);
    }

    @Transactional(readOnly = true)
    public ConteoResponse obtenerPorId(Long conteoId) {
        ConteoInventario conteo = conteoRepository.findById(conteoId)
                .orElseThrow(() -> new EntityNotFoundException("Conteo no encontrado: " + conteoId));
        return toResponse(conteo);
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

    private ConteoResponse toResponse(ConteoInventario c) {
        ConteoResponse res = new ConteoResponse();
        res.setId(c.getId());
        res.setUbicacionTipo(c.getUbicacionTipo());
        res.setUbicacionId(c.getUbicacionId());
        res.setUbicacionNombre(obtenerNombreUbicacion(c.getUbicacionTipo(), c.getUbicacionId()));
        res.setUsuarioId(c.getUsuario().getId());
        res.setUsuarioNombre(c.getUsuario().getNombre());
        res.setFecha(c.getFecha());
        res.setEstado(c.getEstado());

        List<ConteoDetalle> detalles = detalleRepository.findByConteoId(c.getId());
        List<ConteoDetalleResponse> detalleResponses = detalles.stream().map(d -> {
            ConteoDetalleResponse dr = new ConteoDetalleResponse();
            dr.setId(d.getId());
            dr.setProductoId(d.getProducto().getId());
            dr.setProductoNombre(d.getProducto().getNombre());
            dr.setCantidadContada(d.getCantidadContada());
            dr.setCantidadSistema(d.getCantidadSistema());
            dr.setDiferencia(d.getDiferencia());
            return dr;
        }).toList();

        res.setDetalles(detalleResponses);
        return res;
    }
}
