package com.ivera.backend.service;

import com.ivera.backend.dto.*;
import com.ivera.backend.entity.*;
import com.ivera.backend.exception.ReporteIncompletoException;
import com.ivera.backend.enums.UbicacionTipo;
import com.ivera.backend.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private final StockRepository stockRepository;
    private final ConteoDetalleRepository conteoDetalleRepository;
    private final MovimientoRepository movimientoRepository;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final TiendaRepository tiendaRepository;
    private final AlmacenRepository almacenRepository;
    private final ServicioAutorizacionUbicacion autorizacionUbicacion;

    public ReporteService(StockRepository stockRepository,
                          ConteoDetalleRepository conteoDetalleRepository,
                          MovimientoRepository movimientoRepository,
                           VentaDetalleRepository ventaDetalleRepository,
                           TiendaRepository tiendaRepository,
                           AlmacenRepository almacenRepository,
                           ServicioAutorizacionUbicacion autorizacionUbicacion) {
        this.stockRepository = stockRepository;
        this.conteoDetalleRepository = conteoDetalleRepository;
        this.movimientoRepository = movimientoRepository;
        this.ventaDetalleRepository = ventaDetalleRepository;
        this.tiendaRepository = tiendaRepository;
        this.almacenRepository = almacenRepository;
        this.autorizacionUbicacion = autorizacionUbicacion;
    }

    @Transactional(readOnly = true)
    public List<StockBajoResponse> obtenerStockBajo(BigDecimal umbral) {
        BigDecimal limite = (umbral != null) ? umbral : BigDecimal.TEN;
        if (!autorizacionUbicacion.esAdministrador()) {
            Long tiendaId = autorizacionUbicacion.resolverTiendaParaConsulta(null);
            return stockRepository.buscarStockBajoPorUbicacion(UbicacionTipo.TIENDA, tiendaId, limite);
        }
        return stockRepository.buscarStockBajo(limite);
    }

    @Transactional(readOnly = true)
    public List<DiferenciaInventarioResponse> obtenerDiferenciasInventario(LocalDate desde, LocalDate hasta) {
        validarRangoFechas(desde, hasta);
        List<ConteoDetalle> detalles;
        if (autorizacionUbicacion.esAdministrador()) {
            detalles = conteoDetalleRepository.buscarDiferencias(
                    desde != null ? desde.atStartOfDay() : null, hasta != null ? hasta.atTime(23, 59, 59, 999_999_999) : null);
        } else {
            detalles = conteoDetalleRepository.buscarDiferenciasPorUbicacion(UbicacionTipo.TIENDA,
                    autorizacionUbicacion.resolverTiendaParaConsulta(null), desde != null ? desde.atStartOfDay() : null,
                    hasta != null ? hasta.atTime(23, 59, 59, 999_999_999) : null);
        }
        Map<Long, String> nombresTiendas = obtenerNombresTiendas(detalles.stream()
                .map(ConteoDetalle::getConteo)
                .filter(conteo -> conteo.getUbicacionTipo() == UbicacionTipo.TIENDA)
                .map(ConteoInventario::getUbicacionId)
                .collect(Collectors.toSet()));
        Map<Long, String> nombresAlmacenes = obtenerNombresAlmacenes(detalles.stream()
                .map(ConteoDetalle::getConteo)
                .filter(conteo -> conteo.getUbicacionTipo() == UbicacionTipo.ALMACEN)
                .map(ConteoInventario::getUbicacionId)
                .collect(Collectors.toSet()));
        List<DiferenciaInventarioResponse> resultado = new ArrayList<>();

        for (ConteoDetalle d : detalles) {
            if (d.getDiferencia().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            ConteoInventario conteo = d.getConteo();

            DiferenciaInventarioResponse res = new DiferenciaInventarioResponse();
            res.setConteoId(conteo.getId());
            res.setProductoId(d.getProducto().getId());
            res.setProductoNombre(d.getProducto().getNombre());
            res.setUbicacionTipo(conteo.getUbicacionTipo());
            res.setUbicacionId(conteo.getUbicacionId());
            res.setUbicacionNombre(obtenerNombreUbicacion(conteo.getUbicacionTipo(), conteo.getUbicacionId(), nombresTiendas, nombresAlmacenes));
            res.setCantidadContada(d.getCantidadContada());
            res.setCantidadSistema(d.getCantidadSistema());
            res.setDiferencia(d.getDiferencia());
            res.setFecha(conteo.getFecha());

            resultado.add(res);
        }

        return resultado;
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> obtenerMovimientos(Long tiendaId, LocalDate desde, LocalDate hasta) {
        if (tiendaId != null && tiendaId <= 0) {
            throw new IllegalArgumentException("tiendaId debe ser un número positivo");
        }
        if (tiendaId != null && !tiendaRepository.existsById(tiendaId)) {
            throw new IllegalArgumentException("La tienda indicada no existe");
        }
        validarRangoFechas(desde, hasta);
        Long tiendaAutorizada = autorizacionUbicacion.esAdministrador()
                ? tiendaId
                : autorizacionUbicacion.resolverTiendaParaConsulta(tiendaId);
        List<Movimiento> movimientos = movimientoRepository.buscarParaReporte(
                tiendaAutorizada,
                UbicacionTipo.TIENDA,
                desde != null ? desde.atStartOfDay() : null,
                hasta != null ? hasta.atTime(23, 59, 59, 999_999_999) : null
        );
        Map<Long, String> nombresTiendas = obtenerNombresTiendas(obtenerIdsUbicacion(movimientos, UbicacionTipo.TIENDA));
        Map<Long, String> nombresAlmacenes = obtenerNombresAlmacenes(obtenerIdsUbicacion(movimientos, UbicacionTipo.ALMACEN));
        return movimientos.stream()
                .map(movimiento -> toMovimientoResponse(movimiento, nombresTiendas, nombresAlmacenes))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoMasVendidoResponse> obtenerProductosMasVendidos(Integer limite) {
        int limit = limite != null ? limite : 5;
        Long tiendaId = autorizacionUbicacion.esAdministrador() ? null : autorizacionUbicacion.resolverTiendaParaConsulta(null);
        boolean hayHistoricosIncompletos = tiendaId == null
                ? ventaDetalleRepository.existsByCantidadBaseIsNull()
                : ventaDetalleRepository.existsByCantidadBaseIsNullAndVentaTiendaId(tiendaId);
        if (hayHistoricosIncompletos) {
            throw new ReporteIncompletoException(
                    "No se puede calcular el reporte completo porque existen ventas históricas sin cantidad en unidad base"
            );
        }
        return tiendaId == null
                ? ventaDetalleRepository.buscarProductosMasVendidos(PageRequest.of(0, limit))
                : ventaDetalleRepository.buscarProductosMasVendidosPorTienda(tiendaId, PageRequest.of(0, limit));
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
    }

    private Set<Long> obtenerIdsUbicacion(List<Movimiento> movimientos, UbicacionTipo tipo) {
        return movimientos.stream()
                .flatMap(movimiento -> java.util.stream.Stream.of(
                        movimiento.getUbicacionOrigenTipo() == tipo ? movimiento.getUbicacionOrigenId() : null,
                        movimiento.getUbicacionDestinoTipo() == tipo ? movimiento.getUbicacionDestinoId() : null
                ))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Long, String> obtenerNombresTiendas(Set<Long> ids) {
        return tiendaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Tienda::getId, Tienda::getNombre));
    }

    private Map<Long, String> obtenerNombresAlmacenes(Set<Long> ids) {
        return almacenRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Almacen::getId, Almacen::getNombre));
    }

    private String obtenerNombreUbicacion(UbicacionTipo tipo, Long id, Map<Long, String> nombresTiendas, Map<Long, String> nombresAlmacenes) {
        if (tipo == UbicacionTipo.TIENDA) {
            return nombresTiendas.getOrDefault(id, "Desconocida");
        } else if (tipo == UbicacionTipo.ALMACEN) {
            return nombresAlmacenes.getOrDefault(id, "Desconocida");
        }
        return "Desconocida";
    }

    private MovimientoResponse toMovimientoResponse(Movimiento m, Map<Long, String> nombresTiendas, Map<Long, String> nombresAlmacenes) {
        MovimientoResponse res = new MovimientoResponse();
        res.setId(m.getId());
        res.setProductoId(m.getProducto().getId());
        res.setProductoNombre(m.getProducto().getNombre());
        res.setTipo(m.getTipo());
        res.setCantidad(m.getCantidad());
        res.setUnidad(new UnidadResponse(m.getUnidad().getId(), m.getUnidad().getNombre()));
        res.setUbicacionOrigenTipo(m.getUbicacionOrigenTipo());
        res.setUbicacionOrigenId(m.getUbicacionOrigenId());
        res.setUbicacionOrigenNombre(obtenerNombreUbicacion(m.getUbicacionOrigenTipo(), m.getUbicacionOrigenId(), nombresTiendas, nombresAlmacenes));
        res.setUbicacionDestinoTipo(m.getUbicacionDestinoTipo());
        res.setUbicacionDestinoId(m.getUbicacionDestinoId());
        res.setUbicacionDestinoNombre(m.getUbicacionDestinoId() != null ? obtenerNombreUbicacion(m.getUbicacionDestinoTipo(), m.getUbicacionDestinoId(), nombresTiendas, nombresAlmacenes) : null);
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
