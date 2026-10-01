package com.ivera.backend.repository;

import com.ivera.backend.entity.ProductoPresentacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoPresentacionRepository extends JpaRepository<ProductoPresentacion, Long> {
    List<ProductoPresentacion> findByProductoId(Long productoId);
    void deleteByProductoId(Long productoId);
}