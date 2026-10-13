package com.ivera.backend.controller;

import com.ivera.backend.auth.JwtUtil;
import com.ivera.backend.config.SecurityConfig;
import com.ivera.backend.exception.ReporteIncompletoException;
import com.ivera.backend.service.ReporteService;
import com.ivera.backend.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(ReporteController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ReporteControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ReporteService reporteService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @Test
    void rechazaSolicitudSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/reportes/stock-bajo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeConsultarStockBajo() throws Exception {
        when(reporteService.obtenerStockBajo(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/reportes/stock-bajo?umbral=10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TRABAJADOR")
    void trabajadorPuedeConsultarMovimientos() throws Exception {
        when(reporteService.obtenerMovimientos(anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/reportes/movimientos?tiendaId=1&desde=2026-01-01&hasta=2026-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaUmbralNegativo() throws Exception {
        mockMvc.perform(get("/api/reportes/stock-bajo?umbral=-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaLimiteCero() throws Exception {
        mockMvc.perform(get("/api/reportes/productos-mas-vendidos?limite=0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaLimiteNegativo() throws Exception {
        mockMvc.perform(get("/api/reportes/productos-mas-vendidos?limite=-2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void aceptaLimitePositivo() throws Exception {
        when(reporteService.obtenerProductosMasVendidos(3)).thenReturn(List.of());

        mockMvc.perform(get("/api/reportes/productos-mas-vendidos?limite=3"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaRangoDeDiferenciasInvertido() throws Exception {
        when(reporteService.obtenerDiferenciasInventario(any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta"));

        mockMvc.perform(get("/api/reportes/diferencias-inventario?desde=2026-02-02&hasta=2026-02-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void respondeConflictCuandoExistenVentasHistoricasSinCantidadBase() throws Exception {
        String mensaje = "No se puede calcular el reporte completo porque existen ventas históricas sin cantidad en unidad base";
        when(reporteService.obtenerProductosMasVendidos(5)).thenThrow(new ReporteIncompletoException(mensaje));

        mockMvc.perform(get("/api/reportes/productos-mas-vendidos?limite=5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(mensaje));
    }
}
