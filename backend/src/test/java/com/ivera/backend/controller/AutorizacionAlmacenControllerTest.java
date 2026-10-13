package com.ivera.backend.controller;

import com.ivera.backend.auth.JwtUtil;
import com.ivera.backend.config.SecurityConfig;
import com.ivera.backend.dto.AutorizacionAlmacenResponse;
import com.ivera.backend.service.AutorizacionAlmacenService;
import com.ivera.backend.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutorizacionAlmacenController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AutorizacionAlmacenControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AutorizacionAlmacenService autorizacionService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser(roles = "TRABAJADOR")
    void trabajadorNoPuedeConsultarAutorizaciones() throws Exception {
        mockMvc.perform(get("/api/autorizaciones-almacen"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeConsultarAutorizaciones() throws Exception {
        when(autorizacionService.listar(null)).thenReturn(List.of(new AutorizacionAlmacenResponse()));

        mockMvc.perform(get("/api/autorizaciones-almacen"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void duplicadoDeAutorizacionActivaRespondeConflictSinDetalleInterno() throws Exception {
        when(autorizacionService.otorgar(any())).thenThrow(violacionUnica("uk_autorizacion_almacen_activa"));

        mockMvc.perform(post("/api/autorizaciones-almacen")
                        .contentType("application/json")
                        .content("{\"trabajadorId\":1,\"almacenId\":1,\"accion\":\"AJUSTE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe una autorización activa para este trabajador, almacén y acción"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void otraViolacionDeIntegridadNoSeClasificaComoAutorizacionDuplicada() throws Exception {
        when(autorizacionService.otorgar(any())).thenThrow(violacionUnica("uk_otro_recurso"));

        mockMvc.perform(post("/api/autorizaciones-almacen")
                        .contentType("application/json")
                        .content("{\"trabajadorId\":1,\"almacenId\":1,\"accion\":\"AJUSTE\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("No se pudo completar la operación."));
    }

    private DataIntegrityViolationException violacionUnica(String constraint) {
        return new DataIntegrityViolationException("error de prueba", new SQLException(
                "duplicate key value violates unique constraint \"" + constraint + "\"", "23505"));
    }
}
