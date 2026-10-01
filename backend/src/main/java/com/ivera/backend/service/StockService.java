package com.ivera.backend.service;

import com.ivera.backend.dto.StockResponse;
import com.ivera.backend.entity.Almacen;
import com.ivera.backend.entity.Producto;
import com.ivera.backend.entity.Stock;
import com.ivera.backend.entity.Tienda;
import com.ivera.backend.enums.UbicacionTipo;
import com.ivera.backend.repository.AlmacenRepository;
import com.ivera.backend.repository.ProductoRepository;
import com.ivera.backend.repository.StockRepository;
import com.ivera.backend.repository.TiendaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final ProductoRepository productoRepository;
    private final TiendaRepository tiendaRepository;
    private final AlmacenRepository almacenRepository;

    public StockService(StockRepository stockRepository,
                        ProductoRepository productoRepository,
                        TiendaRepository tiendaRepository,
                        AlmacenRepository almacenRepository) {
        this.stockRepository = stockRepository;
        this.productoRepository = productoRepository;
        this.tiendaRepository = tiendaRepository;
        this.almacenRepository = almacenRepository;
    }

    @Transactional(readOnly = true)
    public List<StockResponse> obtenerStockConsolidado(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + productoId));

        List<Stock> stocks = stockRepository.findByProductoId(productoId);
        
        Map<String, BigDecimal> stockMap = stocks.stream().collect(Collectors.toMap(
                s -> s.getUbicacionTipo() + "_" + s.getUbicacionId(),
                Stock::getCantidad,
                (v1, v2) -> v1
        ));

        List<StockResponse> resultado = new ArrayList<>();

        List<Tienda> tiendas = tiendaRepository.findAll();
        for (Tienda tienda : tiendas) {
            String key = UbicacionTipo.TIENDA + "_" + tienda.getId();
            BigDecimal cantidad = stockMap.getOrDefault(key, BigDecimal.ZERO);
            resultado.add(new StockResponse(tienda.getId(), UbicacionTipo.TIENDA, tienda.getNombre(), cantidad));
        }

        List<Almacen> almacenes = almacenRepository.findAll();
        for (Almacen almacen : almacenes) {
            String key = UbicacionTipo.ALMACEN + "_" + almacen.getId();
            BigDecimal cantidad = stockMap.getOrDefault(key, BigDecimal.ZERO);
            resultado.add(new StockResponse(almacen.getId(), UbicacionTipo.ALMACEN, almacen.getNombre(), cantidad));
        }

        return resultado;
    }
}
