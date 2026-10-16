package com.ivera.backend.controller;

import com.ivera.backend.auth.JwtUtil;
import com.ivera.backend.config.SecurityConfig;
import com.ivera.backend.dto.UnidadResponse;
import com.ivera.backend.service.UnidadMedidaService;
import com.ivera.backend.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UnidadMedidaController.class)
@Import(SecurityConfig.class)
class UnidadMedidaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private UnidadMedidaService unidadMedidaService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @Test
    void rechazaSolicitudSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/unidades-medida"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeListarUnidades() throws Exception {
        when(unidadMedidaService.listar()).thenReturn(List.of(new UnidadResponse(1L, "unidad")));

        mockMvc.perform(get("/api/unidades-medida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("unidad"));
    }

    @Test
    @WithMockUser(roles = "TRABAJADOR")
    void trabajadorPuedeListarUnidades() throws Exception {
        when(unidadMedidaService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/unidades-medida"))
                .andExpect(status().isOk());
    }
}
