# Progreso — Ivera

## Estado actual
Sprint 3 — Tarea 3.2 completada

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
- `DataInitializer.java` — crea admin desde variables de entorno `ADMIN_EMAIL` y `ADMIN_PASSWORD` (BCrypt), idempotente. Además crea un usuario de prueba con rol TRABAJADOR desde `TEST_WORKER_EMAIL` y `TEST_WORKER_PASSWORD` (BCrypt) — usuario de prueba, no usar en producción.
- Compilación y empaquetado verificados (`mvn compile`, `mvn package`).

#### Bugs encontrados y corregidos:

1. **Filtro duplicado por `@Component`**: `JwtAuthenticationFilter` estaba anotado con `@Component`, lo que lo registraba en la cadena global de filtros de Tomcat ADEMÁS de la cadena de Spring Security (por `addFilterBefore`). `OncePerRequestFilter` impedía la segunda ejecución, y `SecurityContextHolderFilter` de Spring Security 6 pisaba la autenticación al inicio de su cadena. **Solución:** Quitar `@Component`, declarar el filtro como `@Bean` en `SecurityConfig`.

2. **DispatcherType incorrecto**: Se usó `requestMatchers(DispatcherType.ERROR)` que no es un método válido (solo acepta `RequestMatcher` o `String`). **Solución:** Reemplazar por `.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()` como método independiente en `authorizeHttpRequests`.

Ambos corregidos, `mvn clean compile` → BUILD SUCCESS.

### Sprint 1 — Tarea 1.4: CRUD de Producto con presentaciones
- DTOs: `ProductoRequest`, `PresentacionRequest`, `ProductoResponse`, `PresentacionResponse`, `UnidadResponse`.
- `ProductoService.java` — CRUD completo con creación/actualización de presentaciones, búsqueda por nombre y código de barras, equivalencia calculada (ej. "1 saco = 25 unidad(es) base").
- `ProductoController.java` — endpoints:
  - `GET /api/productos` (listar, con ?nombre= opcional)
  - `GET /api/productos/{id}` (obtener por ID)
  - `GET /api/productos/buscar/codigo?codigo=X` (buscar por código de barras)
  - `POST /api/productos` (crear — solo ADMINISTRADOR)
  - `PUT /api/productos/{id}` (actualizar — solo ADMINISTRADOR)
  - `DELETE /api/productos/{id}` (eliminar — solo ADMINISTRADOR)
- `SecurityConfig.java` — agregado `@EnableMethodSecurity`.
- `ProductoRepository.java` — agregado `findByNombreContainingIgnoreCase` y `findByCodigoBarras`.
- `ProductoPresentacionRepository.java` — agregado `findByProductoId` y `deleteByProductoId`.
- `OpenApiConfig.java` — esquema de seguridad Bearer JWT para Swagger.
- Todos los endpoints validados con `@Valid` y permisos con `@PreAuthorize`.
- **Bug corregido**: Mapeo del campo `unidad` en `PresentacionResponse` dentro de `ProductoService.toPresentacionResponse` para que devuelva el objeto completo (`id` + `nombre`), igual que `unidadBase`.

### Sprint 2 — Tarea 2.1: Entidad Stock, Repositorio y Stock Consolidado
- `UbicacionTipo.java` enum (`TIENDA`, `ALMACEN`).
- `Stock.java` (Entidad JPA con relación a `Producto`, `ubicacionTipo`, `ubicacionId`, `cantidad` en unidad base).
- `StockRepository.java` (repositorio JPA con métodos de búsqueda por producto y ubicación).
- `StockResponse.java` DTO.
- `StockService.java` — lógica para consultar el stock consolidado de un producto por ubicación, retornando todas las tiendas y almacenes (con cantidad 0 si no existe registro previo).
- `StockController.java` — endpoint `GET /api/stock/{productoId}` (accesible por roles `ADMINISTRADOR` y `TRABAJADOR`).
- **Ajuste en `application.yml`**: Fusión de bloques `server:` duplicados para evitar excepciones de carga YAML.

### Sprint 2 — Tarea 2.2: Entidad Movimiento, Repositorio y Registro de Movimientos
- `MovimientoTipo.java` enum (`ENTRADA`, `SALIDA`, `AJUSTE`, `TRASLADO`).
- `MovimientoEstado.java` enum (`PENDIENTE`, `APROBADO`, `RECHAZADO`).
- `Movimiento.java` (Entidad JPA con relaciones a `Producto`, `UnidadMedida`, `Usuario`, ubicaciones de origen/destino, estado, etc.).
- `MovimientoRepository.java`.
- `MovimientoRequest.java` / `MovimientoResponse.java` DTOs.
- `MovimientoService.java` — lógica de negocio:
  - Conversión de cantidad a unidad base (soporta unidad base y presentaciones).
  - Validación de existencia de producto, unidad y ubicaciones.
  - Validación de cantidad positiva y stock suficiente para salidas.
  - `ENTRADA` y `SALIDA` actualizan el Stock inmediatamente y se marcan como `APROBADO`.
  - `AJUSTE` y `TRASLADO` se registran en estado `PENDIENTE` sin modificar el stock.
- `MovimientoController.java` — endpoint `POST /api/movimientos` (accesible por `ADMINISTRADOR` y `TRABAJADOR`).

### Sprint 2 — Tarea 2.3: Aprobación y Rechazo de Movimientos
- `MovimientoService.java` — métodos `aprobar(Long id)` y `rechazar(Long id)`:
  - Validación de estado `PENDIENTE` (lanza error claro si ya fue aprobado o rechazado).
  - Registro del administrador autenticado en `aprobado_por`.
  - Aplicación de efectos sobre `Stock` al aprobar:
    - **TRASLADO**: resta de ubicación origen (validando stock suficiente) y suma a ubicación destino.
    - **AJUSTE**: aplica la diferencia de cantidad (positiva o negativa) en la ubicación origen.
  - Al rechazar: cambia estado a `RECHAZADO` sin modificar stock.
- `MovimientoController.java` — endpoints `PUT /api/movimientos/{id}/aprobar` y `PUT /api/movimientos/{id}/rechazar` (restringidos a `ADMINISTRADOR` mediante `@PreAuthorize("hasRole('ADMINISTRADOR')")`).
- `GlobalExceptionHandler.java` — manejador global de excepciones (`EntityNotFoundException` a 404, `IllegalStateException`/`IllegalArgumentException` a 400).

### Sprint 2 — Tarea 2.4: Conteo Físico de Inventario
- `ConteoEstado.java` enum (`EN_PROCESO`, `FINALIZADO`).
- `ConteoInventario.java` y `ConteoDetalle.java` (Entidades JPA para conteo físico y sus detalles con cálculo de diferencia `cantidadContada - cantidadSistema`).
- `ConteoInventarioRepository.java` y `ConteoDetalleRepository.java`.
- `ConteoRequest.java`, `ConteoDetalleRequest.java`, `ConteoDetalleResponse.java`, `ConteoResponse.java` DTOs.
- `ConteoService.java` — lógica de negocio:
  - Iniciar conteo (`EN_PROCESO`).
  - Agregar/actualizar detalle de producto consultando automáticamente el stock actual en sistema y calculando la diferencia.
  - Finalizar conteo (`FINALIZADO`, sin modificar stock automáticamente).
  - Consultar conteo con todos sus detalles y diferencias.
- `ConteoController.java` — endpoints `POST /api/conteos`, `POST /api/conteos/{id}/detalle`, `PUT /api/conteos/{id}/finalizar`, `GET /api/conteos/{id}` (accesibles por `ADMINISTRADOR` y `TRABAJADOR`).

### Sprint 3 — Tarea 3.1: Proveedores, Compras y Ventas
- `Proveedor.java`, `ProveedorRepository.java`, DTOs, y `ProveedorController.java` (CRUD completo: Admin crea/edita/elimina, ambos roles consultan).
- `Compra.java`, `CompraDetalle.java`, repositorios, DTOs y `CompraController.java`:
  - Endpoint `POST /api/compras` (calcula el total en memoria a partir de los ítems del request antes de persistir la entidad Compra, generando movimientos `ENTRADA` aprobados automáticamente y sumando stock a la tienda indicada).
  - Endpoints `GET /api/compras` (con filtro opcional `?tiendaId=`) y `GET /api/compras/{id}`.
- `Venta.java`, `VentaDetalle.java`, repositorios, DTOs y `VentaController.java`:
  - Endpoint `POST /api/ventas` (validación estricta de stock disponible "todo o nada" antes de aplicar cambios, cálculo de total previo al guardado de la Venta, generación de movimientos `SALIDA` aprobados automáticamente y descuento de stock de la tienda).
  - Endpoints `GET /api/ventas` (con filtro opcional `?tiendaId=`) y `GET /api/ventas/{id}`.

### Sprint 3 — Tarea 3.2: Mapeo de Unidad y Soporte de Código de Barras en Compras y Ventas
- `CompraItemRequest.java` y `VentaItemRequest.java`: agregado soporte para `codigoBarras` como alternativa a `productoId`.
- `CompraService.java` y `VentaService.java`:
  - Método `resolverProducto` robusto: si viene `productoId`, lo busca por ID; si no, si viene `codigoBarras`, lo busca con `productoRepository.findByCodigoBarras(codigoBarras)` (lanzando error claro si no existe); si no viene ninguno, lanza excepción de validación.
  - Verificación del mapeo correcto del campo `unidad` (`UnidadResponse` con `id` y `nombre`) en los detalles de respuesta de compras y ventas.

## Decisiones tomadas durante el desarrollo
- Se agregó un usuario de prueba con rol TRABAJADOR (variables `TEST_WORKER_EMAIL` / `TEST_WORKER_PASSWORD`) para facilitar pruebas de endpoints protegidos por rol. Este usuario no debe usarse en producción.

## Pendientes / dudas
(nada aún)
