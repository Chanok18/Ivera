package com.ivera.backend.auth;

import com.ivera.backend.config.SecurityConfig;
import com.ivera.backend.entity.Usuario;
import com.ivera.backend.enums.Rol;
import com.ivera.backend.repository.UsuarioRepository;
import com.ivera.backend.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @Test
    void devuelveLaSesionDefinidaPorElContratoAlAutenticar() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("admin@ivera.pe");
        usuario.setPasswordHash("hash");
        usuario.setRol(Rol.ADMINISTRADOR);
        when(usuarioRepository.findByEmail("admin@ivera.pe")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-segura", "hash")).thenReturn(true);
        when(jwtUtil.generateToken("admin@ivera.pe", "ADMINISTRADOR")).thenReturn("jwt-de-prueba");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"admin@ivera.pe\",\"password\":\"clave-segura\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-de-prueba"))
                .andExpect(jsonPath("$.email").value("admin@ivera.pe"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    void rechazaCredencialesInvalidasSinExponerDetalles() throws Exception {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"nadie@ivera.pe\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Credenciales inválidas"));
    }

    @Test
    void permitePreflightDesdeAmbosOrigenesLocalesConfigurados() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")));

        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")));
    }

    @Test
    void noAutorizaPreflightDesdeOrigenNoConfigurado() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://no-autorizado.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
