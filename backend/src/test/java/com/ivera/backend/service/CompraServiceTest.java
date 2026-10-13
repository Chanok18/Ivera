package com.ivera.backend.service;

import com.ivera.backend.dto.CompraRequest;
import com.ivera.backend.entity.Proveedor;
import com.ivera.backend.entity.Tienda;
import com.ivera.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {
    @Mock private CompraRepository compraRepository;
    @Mock private CompraDetalleRepository compraDetalleRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private TiendaRepository tiendaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private UnidadMedidaRepository unidadMedidaRepository;
    @Mock private ProductoPresentacionRepository presentacionRepository;
    @Mock private StockRepository stockRepository;
    @Mock private MovimientoRepository movimientoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ServicioAutorizacionUbicacion autorizacionUbicacion;
    @InjectMocks private CompraService compraService;

    @Test
    void rechazaCompraEnOtraTiendaAntesDePersistir() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(1L);
        Tienda tienda = new Tienda();
        tienda.setId(2L);
        CompraRequest request = new CompraRequest();
        request.setProveedorId(1L);
        request.setTiendaId(2L);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(tiendaRepository.findById(2L)).thenReturn(Optional.of(tienda));
        doThrow(new AccessDeniedException("No tiene permiso para operar en esta tienda"))
                .when(autorizacionUbicacion).verificarTienda(2L);

        assertThatThrownBy(() -> compraService.registrar(request))
                .isInstanceOf(AccessDeniedException.class);
        verify(compraRepository, never()).save(any());
        verify(stockRepository, never()).save(any());
    }
}
