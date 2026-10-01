package com.ivera.backend.service;

import com.ivera.backend.dto.*;
import com.ivera.backend.entity.Producto;
import com.ivera.backend.entity.ProductoPresentacion;
import com.ivera.backend.entity.UnidadMedida;
import com.ivera.backend.repository.ProductoPresentacionRepository;
import com.ivera.backend.repository.ProductoRepository;
import com.ivera.backend.repository.UnidadMedidaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoPresentacionRepository presentacionRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

    public ProductoService(ProductoRepository productoRepository,
                           ProductoPresentacionRepository presentacionRepository,
                           UnidadMedidaRepository unidadMedidaRepository) {
        this.productoRepository = productoRepository;
        this.presentacionRepository = presentacionRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(String nombre) {
        List<Producto> productos = (nombre != null && !nombre.isBlank())
                ? productoRepository.findByNombreContainingIgnoreCase(nombre)
                : productoRepository.findAll();
        return productos.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));
        return toResponse(producto);
    }

    @Transactional(readOnly = true)
    public ProductoResponse buscarPorCodigoBarras(String codigo) {
        Producto producto = productoRepository.findByCodigoBarras(codigo)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con código: " + codigo));
        return toResponse(producto);
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        UnidadMedida unidadBase = unidadMedidaRepository.findById(request.getUnidadBaseId())
                .orElseThrow(() -> new EntityNotFoundException("Unidad de medida no encontrada: " + request.getUnidadBaseId()));

        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setCategoria(request.getCategoria());
        producto.setCodigoBarras(request.getCodigoBarras());
        producto.setUnidadBase(unidadBase);
        producto.setImagenUrl(request.getImagenUrl());
        producto.setPrecioUnitario(request.getPrecioUnitario());
        producto = productoRepository.save(producto);

        if (request.getPresentaciones() != null) {
            for (PresentacionRequest pReq : request.getPresentaciones()) {
                ProductoPresentacion pp = new ProductoPresentacion();
                pp.setProducto(producto);
                pp.setUnidad(unidadMedidaRepository.findById(pReq.getUnidadId())
                        .orElseThrow(() -> new EntityNotFoundException("Unidad no encontrada: " + pReq.getUnidadId())));
                pp.setFactorConversion(pReq.getFactorConversion());
                pp.setPrecioPresentacion(pReq.getPrecioPresentacion());
                presentacionRepository.save(pp);
            }
        }

        return toResponse(producto);
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));

        UnidadMedida unidadBase = unidadMedidaRepository.findById(request.getUnidadBaseId())
                .orElseThrow(() -> new EntityNotFoundException("Unidad de medida no encontrada: " + request.getUnidadBaseId()));

        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setCategoria(request.getCategoria());
        producto.setCodigoBarras(request.getCodigoBarras());
        producto.setUnidadBase(unidadBase);
        producto.setImagenUrl(request.getImagenUrl());
        producto.setPrecioUnitario(request.getPrecioUnitario());

        presentacionRepository.deleteByProductoId(producto.getId());

        if (request.getPresentaciones() != null) {
            for (PresentacionRequest pReq : request.getPresentaciones()) {
                ProductoPresentacion pp = new ProductoPresentacion();
                pp.setProducto(producto);
                pp.setUnidad(unidadMedidaRepository.findById(pReq.getUnidadId())
                        .orElseThrow(() -> new EntityNotFoundException("Unidad no encontrada: " + pReq.getUnidadId())));
                pp.setFactorConversion(pReq.getFactorConversion());
                pp.setPrecioPresentacion(pReq.getPrecioPresentacion());
                presentacionRepository.save(pp);
            }
        }

        return toResponse(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));
        presentacionRepository.deleteByProductoId(producto.getId());
        productoRepository.delete(producto);
    }

    private ProductoResponse toResponse(Producto producto) {
        ProductoResponse res = new ProductoResponse();
        res.setId(producto.getId());
        res.setNombre(producto.getNombre());
        res.setDescripcion(producto.getDescripcion());
        res.setCategoria(producto.getCategoria());
        res.setCodigoBarras(producto.getCodigoBarras());
        res.setUnidadBase(new UnidadResponse(producto.getUnidadBase().getId(), producto.getUnidadBase().getNombre()));
        res.setImagenUrl(producto.getImagenUrl());
        res.setPrecioUnitario(producto.getPrecioUnitario());

        List<ProductoPresentacion> presentaciones = presentacionRepository.findByProductoId(producto.getId());
        res.setPresentaciones(presentaciones.stream().map(this::toPresentacionResponse).toList());

        return res;
    }

    private PresentacionResponse toPresentacionResponse(ProductoPresentacion pp) {
        PresentacionResponse res = new PresentacionResponse();
        res.setId(pp.getId());
        res.setUnidad(new UnidadResponse(pp.getUnidad().getId(), pp.getUnidad().getNombre()));
        res.setFactorConversion(pp.getFactorConversion());
        res.setPrecioPresentacion(pp.getPrecioPresentacion());
        res.setEquivalencia("1 " + pp.getUnidad().getNombre() + " = " + pp.getFactorConversion()
                + " " + pp.getProducto().getUnidadBase().getNombre() + "(s) base");
        return res;
    }
}