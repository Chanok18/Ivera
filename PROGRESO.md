# Progreso — Ivera

## Estado actual
Sprint 1 — Tareas 1.1, 1.2 y 1.3 completadas

## Hecho
### Sprint 1 — Tarea 1.1: Proyecto base
- `pom.xml` con dependencias: web, data-jpa, postgresql, security, validation, springdoc-openapi.
- `application.yml` con variables de entorno (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`).
- `HealthController.java` — `GET /api/health` público.
- `SecurityConfig.java` — Spring Security stateless, permite health y swagger sin auth.
- Compilación verificada (`mvn compile`).

### Sprint 1 — Tarea 1.2: Entidades JPA, repositorios y datos iniciales
- Entidades creadas: `Tienda`, `Almacen`, `Usuario`, `UnidadMedida`, `Producto`, `ProductoPresentacion`.
- Repositorios: `TiendaRepository`, `AlmacenRepository`, `UsuarioRepository`, `UnidadMedidaRepository`, `ProductoRepository`, `ProductoPresentacionRepository`.
- `DataInitializer` idempotente (`CommandLineRunner` con verificación `existsByNombre`) que carga:
  - 6 unidades de medida: unidad, docena, centena, millar, saco, metro
  - 2 tiendas: Romel y Todopernos
  - 1 almacén: Almacén Central
- `ddl-auto: update` en `application.yml`.
- Tablas verificadas en BD PostgreSQL y datos cargados sin duplicados.

### Sprint 1 — Tarea 1.3: Login JWT y roles
- Dependencias jjwt agregadas a `pom.xml` (api, impl, jackson 0.12.6).
- `JwtUtil.java` — genera y valida tokens con HMAC-SHA256, claims: subject (email), rol.
- `LoginRequest.java` / `LoginResponse.java` — DTOs con validación.
- `AuthController.java` — `POST /api/auth/login` que retorna token + email + rol.
- `UserDetailsServiceImpl.java` — carga usuario desde BD vía email.
- `JwtAuthenticationFilter.java` — filtro `OncePerRequestFilter` que extrae JWT del header `Authorization: Bearer ...`.
- `SecurityConfig.java` — `/api/health`, `/api/auth/login`, Swagger públicos; resto requiere token; sin contraseña generada por defecto.
- `DataInitializer.java` — crea admin desde variables de entorno `ADMIN_EMAIL` y `ADMIN_PASSWORD` (BCrypt), idempotente.
- Compilación y empaquetado verificados (`mvn compile`, `mvn package`).

#### Bugs encontrados y corregidos:

1. **Filtro duplicado por `@Component`**: `JwtAuthenticationFilter` estaba anotado con `@Component`, lo que lo registraba en la cadena global de filtros de Tomcat ADEMÁS de la cadena de Spring Security (por `addFilterBefore`). `OncePerRequestFilter` impedía la segunda ejecución, y `SecurityContextHolderFilter` de Spring Security 6 pisaba la autenticación al inicio de su cadena. **Solución:** Quitar `@Component`, declarar el filtro como `@Bean` en `SecurityConfig`.

2. **DispatcherType incorrecto**: Se usó `requestMatchers(DispatcherType.ERROR)` que no es un método válido (solo acepta `RequestMatcher` o `String`). **Solución:** Reemplazar por `.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()` como método independiente en `authorizeHttpRequests`.

Ambos corregidos, `mvn clean compile` → BUILD SUCCESS.

## Decisiones tomadas durante el desarrollo
(nada aún)

## Pendientes / dudas
(nada aún)
