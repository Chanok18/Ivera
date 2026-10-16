package com.ivera.backend.controller;

import com.ivera.backend.auth.JwtUtil;
import com.ivera.backend.config.SecurityConfig;
import com.ivera.backend.service.ProductoService;
import com.ivera.backend.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ProductoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ProductoService productoService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser(roles = "TRABAJADOR")
    void trabajadorNoPuedeCrearProductos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Perno\",\"unidadBaseId\":1,\"precioUnitario\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorRecibeValidacionParaCamposObligatorios() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"precioUnitario\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorRecibeValidacionParaFactorDePresentacionNoPositivo() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Perno\",\"unidadBaseId\":1,\"precioUnitario\":1,\"presentaciones\":[{\"unidadId\":1,\"factorConversion\":0}]}"))
                .andExpect(status().isBadRequest());
    }
}
