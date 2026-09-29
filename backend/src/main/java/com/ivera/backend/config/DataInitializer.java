package com.ivera.backend.config;

import com.ivera.backend.entity.Almacen;
import com.ivera.backend.entity.Tienda;
import com.ivera.backend.entity.UnidadMedida;
import com.ivera.backend.entity.Usuario;
import com.ivera.backend.enums.Rol;
import com.ivera.backend.repository.AlmacenRepository;
import com.ivera.backend.repository.TiendaRepository;
import com.ivera.backend.repository.UnidadMedidaRepository;
import com.ivera.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UnidadMedidaRepository unidadMedidaRepository;
    private final TiendaRepository tiendaRepository;
    private final AlmacenRepository almacenRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    public DataInitializer(UnidadMedidaRepository unidadMedidaRepository,
                           TiendaRepository tiendaRepository,
                           AlmacenRepository almacenRepository,
                           UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.tiendaRepository = tiendaRepository;
        this.almacenRepository = almacenRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        cargarUnidadesMedida();
        cargarTiendas();
        cargarAlmacen();
        cargarAdmin();
    }

    private void cargarUnidadesMedida() {
        String[] unidades = {"unidad", "docena", "centena", "millar", "saco", "metro"};
        for (String nombre : unidades) {
            if (!unidadMedidaRepository.existsByNombre(nombre)) {
                UnidadMedida u = new UnidadMedida();
                u.setNombre(nombre);
                unidadMedidaRepository.save(u);
            }
        }
    }

    private void cargarTiendas() {
        String[][] tiendas = {
                {"Romel", "20123456789", "Av. Principal 123"},
                {"Todopernos", "20987654321", "Jr. Secundaria 456"}
        };
        for (String[] t : tiendas) {
            if (!tiendaRepository.existsByNombre(t[0])) {
                Tienda tienda = new Tienda();
                tienda.setNombre(t[0]);
                tienda.setRuc(t[1]);
                tienda.setDireccion(t[2]);
                tiendaRepository.save(tienda);
            }
        }
    }

    private void cargarAlmacen() {
        if (!almacenRepository.existsByNombre("Almacén Central")) {
            Almacen a = new Almacen();
            a.setNombre("Almacén Central");
            almacenRepository.save(a);
        }
    }

    private void cargarAdmin() {
        if (!usuarioRepository.existsByEmail(adminEmail)) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRol(Rol.ADMINISTRADOR);
            usuarioRepository.save(admin);
        }
    }
}