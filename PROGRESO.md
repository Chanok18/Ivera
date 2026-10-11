# Progreso — Ivera

## Estado actual
Sprint 4 — Tarea 4.3.2 pendiente de aprobación técnica; Tarea 4.3.1 pendiente de validación de concurrencia en PostgreSQL aislado

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

### Sprint 4 — Tarea 4.1: Reportes del Sistema
- DTOs de reportes: `StockBajoResponse`, `DiferenciaInventarioResponse`, `ProductoMasVendidoResponse`.
- `ReporteService.java` — lógica de negocio para los 4 reportes:
  - Stock bajo (`?umbral=X`, default 10).
  - Diferencias de inventario en conteos físicos (filtrando diferencia != 0 y rango de fechas ISO `desde`/`hasta`).
  - Movimientos filtrados por tienda y rango de fechas (ordenados del más reciente al más antiguo).
  - Productos más vendidos (Top N, default 5).
- `ReporteController.java` — endpoints `GET /api/reportes/stock-bajo`, `GET /api/reportes/diferencias-inventario`, `GET /api/reportes/movimientos`, `GET /api/reportes/productos-mas-vendidos` (accesibles por `ADMINISTRADOR` y `TRABAJADOR`).

### Sprint 4 — Validación de Tarea 4.1
- Qué se revisó: seguridad JWT global y por método, permisos de `ADMINISTRADOR` y `TRABAJADOR`, DTOs expuestos, lógica de los cuatro reportes, filtros, ordenamiento y repositorios utilizados.
- Pruebas ejecutadas realmente: revisión estática de `ReporteController`, `ReporteService`, `SecurityConfig`, entidades, DTOs y repositorios. `mvn clean test` finalizó con `BUILD SUCCESS`, pero no ejecutó pruebas porque no existe `src/test`. `mvn clean compile` finalizó con `BUILD SUCCESS` (88 archivos fuente compilados).
- Pruebas pendientes: pruebas de integración autenticadas contra un entorno de pruebas con datos controlados para los cuatro endpoints, incluyendo ambos roles, JWT ausente/inválido, suma de las tres ubicaciones, filtros de fecha/tienda, ordenamiento, límites y parámetros inválidos. No se inició la aplicación ni se consultó o alteró PostgreSQL para proteger datos existentes.
- Errores o riesgos encontrados:
  - No se rechazan parámetros inválidos: `umbral` negativo se acepta; `limite <= 0` se reemplaza silenciosamente por 5; `tiendaId` negativo se procesa; y no se valida que `desde` sea menor o igual que `hasta`.
  - `productos-mas-vendidos` suma la cantidad registrada en `VentaDetalle`, que puede estar en una presentación distinta de la unidad base. Por tanto, un saco y una unidad se comparan como cantidades homogéneas aunque no lo sean.
  - Los cuatro reportes cargan colecciones completas con `findAll()` y filtran/agregan en memoria. Stock bajo ejecuta una consulta adicional por producto; diferencias y movimientos ejecutan consultas adicionales por ubicaciones. Esto genera N+1 y puede degradarse con datos reales.
  - No hay pruebas automatizadas para prevenir regresiones ni demostrar los resultados funcionales requeridos.
- Resultado de compilación: `mvn clean compile` -> `BUILD SUCCESS`.
- Estado final: REQUIERE CORRECCIONES.

### Sprint 4 — Tarea 4.2: Corrección y optimización de reportes
- Problemas corregidos:
  - Validación HTTP 400 para `umbral` negativo, `limite` cero/negativo y `tiendaId` no positivo.
  - Validación de rangos de fechas invertidos y verificación de existencia de tienda al filtrar movimientos.
  - `stock-bajo` calcula el total con agregación SQL por producto, sin consultas por producto.
  - Diferencias y movimientos filtran y ordenan en PostgreSQL mediante JPQL; las relaciones requeridas se cargan con `join fetch` y los nombres de ubicaciones se resuelven por lotes.
- Archivos modificados: `ReporteController.java`, `ReporteService.java`, `GlobalExceptionHandler.java`, `StockRepository.java`, `ConteoDetalleRepository.java`, `MovimientoRepository.java` y `pom.xml`.
- Archivos creados: `ReporteControllerTest.java`, `ReporteServiceTest.java` y `StockRepositoryTest.java`.
- Validaciones implementadas: formato ISO de fechas existente, límites numéricos positivos, umbral no negativo, rango `desde <= hasta`, y tienda existente cuando se filtra por `tiendaId`.
- Consultas optimizadas: se eliminaron los `findAll()` de stock bajo, diferencias y movimientos. Se mantienen consultas acotadas por filtros, agregación en base de datos y carga anticipada de producto, unidad y usuarios para movimientos.
- Pruebas ejecutadas: `mvn clean test` -> 16 pruebas ejecutadas, 16 exitosas, 0 fallidas, 0 errores. Incluye pruebas MVC de autenticación/roles/parámetros, pruebas unitarias de filtros/rangos/orden y una prueba JPA aislada con H2 en memoria para la suma de las tres ubicaciones. No se accedió a Railway.
- Resultados de verificación: `mvn clean compile` -> `BUILD SUCCESS`; `mvn package` -> `BUILD SUCCESS` y JAR generado.
- Decisión técnica pendiente: no se corrigió el cálculo histórico de `productos-mas-vendidos`. `VentaDetalle` guarda cantidad y unidad original, pero no el factor de conversión vigente al momento de la venta. Como `ProductoPresentacion.factorConversion` puede modificarse, usar el valor actual alteraría el historial. Se requiere aprobación para persistir una cantidad base inmutable por detalle en ventas futuras y definir cómo presentar o regularizar las ventas históricas, sin migración automática.
- Problemas pendientes: falta una prueba de integración contra PostgreSQL para validar las consultas JPQL en el mismo motor de producción y queda pendiente la decisión sobre cantidades base históricas de ventas.
- Estado de la tarea: PENDIENTE DE APROBACIÓN.

### Sprint 4 — Tarea 4.2.1: Persistencia histórica de cantidades vendidas
- Problema original: `VentaDetalle` conservaba cantidad y unidad original, pero no la cantidad equivalente en unidad base. Los factores de presentación pueden cambiar y no permiten reconstruir ventas históricas con certeza.
- Decisión técnica aprobada aplicada: se agregó `cantidadBase` nullable a `VentaDetalle`, con `NUMERIC(10,2)`, para capturar la conversión al confirmar nuevas ventas sin modificar cantidad, unidad o precio originales.
- Decisión aprobada de contrato: cuando existe al menos un `VentaDetalle` histórico con `cantidadBase = NULL`, `GET /api/reportes/productos-mas-vendidos` responde HTTP `409 Conflict` con el formato existente `{"error":"..."}`. El caso HTTP 200 conserva `List<ProductoMasVendidoResponse>`.
- Archivos modificados: `VentaDetalle.java`, `VentaService.java`, `ReporteService.java`, `VentaDetalleRepository.java`, `GlobalExceptionHandler.java`, `ReporteServiceTest.java` y `ReporteControllerTest.java`.
- Archivos creados: `ReporteIncompletoException.java`, `VentaServiceTest.java` y `VentaDetalleRepositoryTest.java`.
- Migración preparada: `backend/src/main/resources/db/migration/V4_2_1__add_venta_detalle_cantidad_base.sql`. Es aditiva, no destructiva, admite `NULL` para históricos y no se ejecutó en Railway ni se integra con una herramienta de migración.
- Implementación: `VentaService` calcula la cantidad base una sola vez por ítem con la lógica existente y reutiliza ese valor para validar stock, persistir el detalle, registrar el movimiento y descontar stock dentro de la transacción existente. El reporte verifica primero históricos incompletos y, si no existen, agrega exclusivamente `cantidadBase` en base de datos con límite aplicado.
- Pruebas ejecutadas: `mvn clean test` -> 25 pruebas ejecutadas, 25 exitosas, 0 fallidas, 0 errores. Se cubren unidad base, presentación por docena, venta combinada, captura inmutable ante cambio posterior del factor, ausencia de escrituras ante falla de prevalidación, agregación homogénea, detección de históricos nulos y HTTP 409.
- Resultados: `mvn clean compile` -> `BUILD SUCCESS`; `mvn package` -> `BUILD SUCCESS` y JAR generado.
- Riesgos pendientes: la migración SQL debe revisarse y ejecutarse manualmente en PostgreSQL antes del despliegue; no se ejecutó contra Railway. Falta una prueba de integración con PostgreSQL real, por lo que las pruebas JPA se ejecutan únicamente contra H2 en memoria.
- Estado de la tarea: COMPLETADA.

### Sprint 4 — Tarea 4.3: Auditoría de seguridad y permisos
- Matriz de permisos actual:

| Método | Ruta | Roles actuales | Documentación / observación |
|---|---|---|---|
| GET | `/api/health` | Público | Autorizado como health público. |
| POST | `/api/auth/login` | Público | Autorizado para login JWT. |
| GET | `/api-docs/**`, `/swagger-ui/**`, `/v3/api-docs/**` | Público | Swagger permitido por `SecurityConfig`. |
| GET | `/api/productos`, `/{id}`, `/buscar/codigo` | ADMINISTRADOR, TRABAJADOR | Compatible: ambos pueden ver productos. |
| POST, PUT, DELETE | `/api/productos`, `/{id}` | ADMINISTRADOR | Compatible: gestión total de catálogo para administrador. |
| GET | `/api/proveedores`, `/{id}` | ADMINISTRADOR, TRABAJADOR | No contradictorio; documentación no detalla consulta de proveedores. |
| POST, PUT, DELETE | `/api/proveedores`, `/{id}` | ADMINISTRADOR | Compatible con acceso total administrativo. |
| GET | `/api/stock/{productoId}` | ADMINISTRADOR, TRABAJADOR | Trabajador puede consultar consolidado; no existe restricción por tienda. |
| POST | `/api/movimientos` | ADMINISTRADOR, TRABAJADOR | Compatible: ambos registran movimientos; entrada/salida se aprueban automáticamente, ajuste/traslado quedan pendientes. |
| PUT | `/api/movimientos/{id}/aprobar`, `/rechazar` | ADMINISTRADOR | Compatible: aprobación sensible solo administrativa. |
| POST, POST, PUT, GET | `/api/conteos`, `/{id}/detalle`, `/{id}/finalizar`, `/{id}` | ADMINISTRADOR, TRABAJADOR | Compatible con conteos por trabajador; sin ámbito por tienda. |
| POST, GET | `/api/compras`, `/{id}` | ADMINISTRADOR, TRABAJADOR | Compatible con registro de compras; filtros y datos no están limitados por tienda del usuario. |
| POST, GET | `/api/ventas`, `/{id}` | ADMINISTRADOR, TRABAJADOR | Compatible con registro de ventas; filtros y datos no están limitados por tienda del usuario. |
| GET | `/api/reportes/*` | ADMINISTRADOR, TRABAJADOR | Aprobado en tarea 4.1; sin ámbito por tienda salvo filtro opcional de movimientos. |
- Seguridad JWT: la cadena es stateless y exige autenticación para toda ruta no pública. El filtro valida firma y expiración y recarga el usuario/autoridades desde BD, por lo que el claim de rol del token no concede privilegios por sí mismo. No se encontraron contraseñas, hashes, secretos ni tokens en DTOs de negocio; el token solo se entrega en la respuesta de login esperada. El filtro registra email y rol en logs, lo que supone exposición de PII operacional.
- Reglas de movimiento verificadas: ENTRADA y SALIDA se guardan `APROBADO` y alteran stock; AJUSTE y TRASLADO se guardan `PENDIENTE` y no alteran stock hasta aprobarse. Aprobar y rechazar exigen `PENDIENTE`; por ello un aprobado/rechazado no puede volver a aprobarse. Los flujos pendientes resuelven el administrador autenticado en `aprobadoPor`; para ENTRADA/SALIDA automáticas, ese campo contiene al usuario que ejecutó el movimiento, incluso si es trabajador.
- Problemas y riesgos encontrados:
  - CRÍTICO: `Stock` no tiene `@Version`, bloqueo pesimista ni actualización atómica. Ventas, salidas, compras y aprobaciones hacen leer-modificar-guardar; peticiones simultáneas pueden perder actualizaciones, sobre-vender o duplicar el efecto de un traslado/ajuste.
  - CRÍTICO: aprobar/rechazar carece de bloqueo/versionado. Dos administradores pueden leer `PENDIENTE` y ambos aplicar efectos de stock; una carrera aprobar/rechazar puede dejar stock y estado final inconsistentes.
  - ALTO: no hay autorización por tienda. Un usuario asociado a Romel puede crear ventas, compras, conteos, entradas/salidas o traslados sobre Todopernos y consultar ventas, compras, conteos, stock consolidado y reportes de otras ubicaciones usando IDs conocidos.
  - ALTO: una venta con varias líneas del mismo producto valida cada línea contra el mismo stock inicial y no acumula el requerido; puede dejar stock negativo aun sin concurrencia.
  - MEDIO: `DataInitializer` crea un trabajador de prueba si existen las variables de entorno, sin perfil ni salvaguarda de producción.
  - MEDIO: no hay pruebas automatizadas para aprobaciones/rechazos, límites por tienda, entradas/salidas por rol ni concurrencia. `server.error.include-message: always` puede revelar detalles de errores de framework no manejados.
- Decisiones pendientes: definir si el vínculo `Usuario.tienda` restringe operaciones y lecturas a esa tienda, qué operaciones deben incluir almacén central o consolidado, si administradores pueden administrar tiendas ajenas y la política de trabajador de prueba en producción. No se implementaron reglas por no estar definidas en la documentación.
- Pruebas ejecutadas: revisión estática de configuración JWT, controladores, servicios, repositorios, entidades y DTOs; `mvn clean test` -> 25 exitosas, 0 fallidas, 0 errores; `mvn clean compile` -> `BUILD SUCCESS`. Las pruebas existentes cubren autenticación y roles únicamente para reportes, no el conjunto de permisos ni concurrencia.
- Estado final: REQUIERE CORRECCIONES.

### Sprint 4 — Tarea 4.3.1: Integridad transaccional y concurrencia
- Estrategia aplicada: bloqueo pesimista `PESSIMISTIC_WRITE` sobre `Producto` antes de toda modificación de stock. Las ventas y compras bloquean los productos por ID ascendente para mantener un orden global y reducir la posibilidad de deadlocks. No se requirió migración ni cambio de esquema.
- `MovimientoService` bloquea el producto al registrar ENTRADA/SALIDA y al aprobar un movimiento. Aprobar y rechazar primero obtienen el movimiento con bloqueo pesimista, por lo que una sola transición puede consumir el estado `PENDIENTE`.
- `VentaService` ahora agrupa la cantidad requerida en unidad base por `(productoId, TIENDA, tiendaId)` y valida el total antes de persistir. Dos líneas del mismo producto ya no pueden sobrepasar el stock disponible de forma independiente.
- `ProductoRepository` y `MovimientoRepository` incluyen `findByIdForUpdate`. `GlobalExceptionHandler` traduce excepciones JPA y de Spring por lock a HTTP `409 Conflict` con un mensaje de reintento.
- Pruebas agregadas: venta con líneas repetidas que exceden el stock, orden de bloqueo del producto antes de consultar stock, aprobación única con actualización única de stock, rechazo único sin modificar stock y rechazo de movimiento ya rechazado.
- Validaciones ejecutadas: `mvn clean test` -> 30 pruebas exitosas, 0 fallidas, 0 errores; `mvn clean compile` -> `BUILD SUCCESS`; `mvn package` -> `BUILD SUCCESS` y JAR generado.
- Limitación pendiente: las pruebas JPA disponibles usan H2 y las pruebas nuevas de servicios usan mocks; verifican la lógica y el orden de acceso, no la semántica real de lock de PostgreSQL. No se tocó Railway. Docker Desktop no está activo en este equipo, por lo que no se pudo iniciar PostgreSQL aislado.
- Pendiente de validación en PostgreSQL aislado: dos ventas concurrentes sin stock negativo, dos aprobaciones del mismo movimiento, aprobar/rechazar simultáneos, traslado concurrente con venta y creación concurrente de stock inexistente. La confirmación de esta batería requiere habilitar Docker o proporcionar una base PostgreSQL de pruebas no productiva.
- Estado de la tarea: IMPLEMENTADA, PENDIENTE DE VALIDACIÓN EN POSTGRESQL.

### Sprint 4 — Tarea 4.3.2: Diseño de permisos por ubicación
- Alcance: auditoría y diseño solamente. No se modificaron entidades, migraciones, endpoints, reglas de negocio, Railway ni contratos de API.
- Decisiones aprobadas incorporadas al diseño: existen únicamente los roles `ADMINISTRADOR` y `TRABAJADOR`; ambos administradores operan globalmente; cada trabajador queda limitado a `Usuario.tienda`; una autorización explícita, revocable y por operación permite a un trabajador operar el almacén central sin darle acceso a otra tienda; y aprobar/rechazar movimientos permanece exclusivo de administrador.
- Auditoría actual: `Usuario` ya tiene `tienda_id` nullable, pero `UserDetailsServiceImpl` solo carga la autoridad de rol y ningún servicio compara la tienda del usuario con los IDs recibidos. `SecurityConfig` exige autenticación global y los controladores usan únicamente `hasRole`/`hasAnyRole`. No existe entidad, repositorio, claim JWT ni servicio reutilizable de permisos por ubicación. El filtro JWT recarga el rol desde base de datos, por lo que la futura autorización debe igualmente resolver el usuario actual desde base de datos y no confiar en un `tiendaId`, almacén o permiso enviado por cliente.
- Superficies auditadas que aceptan o derivan ubicación: `POST /api/ventas` y `POST /api/compras` reciben `tiendaId`; sus listados aceptan `?tiendaId`; `POST /api/movimientos` recibe ubicación origen y destino (tipo e ID); `POST /api/conteos` recibe ubicación tipo e ID; las operaciones posteriores de conteo derivan la ubicación desde el conteo; `GET /api/reportes/movimientos` recibe `?tiendaId`; `GET /api/stock/{productoId}` devuelve todas las tiendas y almacenes; y los otros reportes actualmente agregan datos de todas las ubicaciones.
- Exposición actual: un trabajador puede registrar, listar y consultar ventas/compras de otra tienda por ID; iniciar o modificar conteos ajenos o de almacén; registrar movimientos desde/hacia ubicaciones no asignadas; y obtener reportes consolidados. Los endpoints de aprobar/rechazar ya están limitados a administrador.
- Diseño recomendado: crear en la siguiente tarea un `ServicioAutorizacionUbicacion`, invocado dentro de los servicios transaccionales antes de leer o mutar datos de una ubicación. Debe resolver el `Usuario` autenticado por email, cargar su rol y tienda desde base de datos y exponer verificaciones de tienda propia, operación de almacén y alcance de lectura. El administrador retorna permitido; el trabajador requiere una tienda asignada e ID de tienda coincidente, o una autorización activa de almacén para la acción solicitada. Los IDs del request solo identifican el recurso a validar: nunca determinan el alcance permitido.
- Diseño de persistencia recomendado: tabla independiente `autorizacion_almacen_trabajador`, no un booleano en `usuario`. Campos propuestos: `id`, `usuario_id`, `almacen_id`, `accion`, `otorgado_por_usuario_id`, `otorgado_en`, `revocado_por_usuario_id`, `revocado_en` y `motivo_revocacion` opcional. `accion` debe limitarse a las operaciones aprobadas `CONTEO`, `AJUSTE` y `TRASLADO_ORIGEN`; no es un rol. Se requiere unicidad de autorización activa por `(usuario_id, almacen_id, accion)` mediante índice parcial de PostgreSQL y FKs a usuario/almacén. Esta opción conserva auditoría de otorgamiento/revocación y permite permisos independientes; un campo booleano no satisface operaciones determinadas, historial ni trazabilidad.
- Administración futura de autorizaciones: solo un administrador autenticado podrá otorgar o revocar. El servicio obtiene `otorgadoPor`/`revocadoPor` desde el principal autenticado, verifica que el destinatario sea `TRABAJADOR`, rechaza que el actor se autorice a sí mismo y no acepta esos campos en el request. Otorgar/revocar debe auditarse y ser transaccional. Los endpoints y DTOs administrativos necesarios se diseñarán recién tras aprobación técnica.
- Orden de validación propuesto: resolver el recurso/ubicación con los repositorios existentes para conservar `404` cuando no existe; después aplicar autorización y devolver `403` si existe pero el usuario autenticado no puede acceder. La ausencia o invalidez del JWT permanece `401` por Spring Security. Estados inválidos, stock insuficiente y conflictos de concurrencia conservan `409` cuando correspondan; la validación de request permanece `400`. Actualmente `ReporteService` responde `400` para `tiendaId` inexistente, lo que es inconsistente con el patrón de `EntityNotFoundException` a `404` y debe decidirse/normalizarse al implementar.

#### Matriz propuesta de endpoints

| Método y endpoint | Ubicación afectada | Administrador | Trabajador | Validación futura | Denegado / inexistente |
|---|---|---|---|---|---|
| `GET /api/productos`, `/{id}`, `/buscar/codigo` | Catálogo global | Global | Lectura global | Rol autenticado | 401 sin JWT; 404 producto inexistente |
| `POST/PUT/DELETE /api/productos` | Catálogo global | Permitido | No permitido | Rol administrador | 403 trabajador; 404 recurso inexistente |
| `GET /api/stock/{productoId}` | Stock de todas las ubicaciones | Global | Lectura global aprobada | Validar producto | 401 sin JWT; 404 producto inexistente |
| `POST /api/ventas` | `request.tiendaId` | Cualquier tienda | Solo su tienda | Resolver usuario actual y comparar tienda asignada | 403 tienda ajena; 404 tienda inexistente |
| `GET /api/ventas?tiendaId=` | Ventas por tienda | Global, filtro opcional | Solo su tienda | Para trabajador forzar su tienda; ID ajeno no debe ampliar alcance | 403 filtro ajeno; 404 tienda inexistente |
| `GET /api/ventas/{id}` | Tienda de la venta | Global | Solo si venta pertenece a su tienda | Resolver venta y autorizar por su tienda | 403 venta ajena; 404 venta inexistente |
| `POST /api/compras` | `request.tiendaId` | Cualquier tienda | Solo su tienda | Resolver usuario y comparar tienda asignada | 403 tienda ajena; 404 tienda/proveedor inexistente |
| `GET /api/compras?tiendaId=` | Compras por tienda | Global, filtro opcional | Solo su tienda | Para trabajador forzar su tienda | 403 filtro ajeno; 404 tienda inexistente |
| `GET /api/compras/{id}` | Tienda de la compra | Global | Solo si compra pertenece a su tienda | Resolver compra y autorizar por su tienda | 403 compra ajena; 404 compra inexistente |
| `POST /api/conteos` | `ubicacionTipo` + `ubicacionId` | Cualquier ubicación | Tienda propia; almacén con `CONTEO` activo | Validar ubicación y permiso por acción | 403 sin alcance; 404 ubicación inexistente |
| `POST /api/conteos/{id}/detalle`, `PUT /api/conteos/{id}/finalizar`, `GET /api/conteos/{id}` | Ubicación del conteo persistido | Global | Mismo alcance vigente sobre la ubicación del conteo | No confiar en body ni en creador; cargar conteo y autorizar su ubicación | 403 sin alcance; 404 conteo inexistente |
| `POST /api/movimientos` de tipo `AJUSTE` | Ubicación origen | Cualquier ubicación | Tienda propia; almacén con `AJUSTE` activo | Validar tipo, ubicación y acción antes de registrar | 403 sin alcance; 404 ubicación/producto/unidad inexistente |
| `POST /api/movimientos` de tipo `TRASLADO` | Ubicación origen y destino | Cualquier ubicación | Puede solicitar desde tienda propia; desde almacén con `TRASLADO_ORIGEN` activo | Autorizar siempre origen; permiso sobre destino entre tiendas requiere decisión pendiente | 403 sin alcance; 404 ubicación inexistente |
| `POST /api/movimientos` de tipo `ENTRADA` o `SALIDA` | Ubicación origen | Global | Pendiente de definición | La política aprobada no define el alcance de trabajador para estos tipos | No implementar hasta decisión |
| `PUT /api/movimientos/{id}/aprobar`, `/rechazar` | Movimiento y sus ubicaciones | Global | Nunca permitido | `hasRole('ADMINISTRADOR')` ya existe; servicio mantiene validación de estado | 403 trabajador; 404 movimiento inexistente; 409 conflicto de lock/estado según contrato futuro |
| `GET /api/reportes/stock-bajo` | Actualmente consolidado | Global | Solo datos de su tienda | Consulta debe filtrar stock por tienda asignada | 403 sin tienda asignada; 401 sin JWT |
| `GET /api/reportes/diferencias-inventario` | Conteos por ubicación | Global | Solo conteos de su tienda | Filtrar por `ConteoInventario` de tienda asignada | 403 sin tienda asignada; 401 sin JWT |
| `GET /api/reportes/movimientos?tiendaId=` | Movimientos por tienda | Global, filtro opcional | Solo su tienda | Forzar tienda propia; no permitir otra tienda ni almacén no contemplado | 403 filtro ajeno; 404 tienda inexistente |
| `GET /api/reportes/productos-mas-vendidos` | Ventas por tienda | Global | Solo ventas de su tienda | Agregar únicamente detalles de ventas de tienda asignada | 403 sin tienda asignada; 409 históricos incompletos ya definido |

- Casos especiales y decisiones pendientes:
  - Trabajador sin tienda asignada: debe recibir `403` para toda operación o reporte con alcance de tienda/almacén, aunque tenga una autorización de almacén. Se requiere confirmar si puede conservar solo lectura global de catálogo y stock, como indica la política; el diseño lo mantiene permitido.
  - Revocación de almacén: aplica a solicitudes posteriores y a toda continuación de conteo o movimiento que vuelva a pasar por autorización. Se requiere confirmar si debe bloquear también la finalización de un conteo iniciado antes de revocar; el diseño recomendado la bloquea para evitar que una autorización revocada siga produciendo efectos.
  - Traslado tienda-almacén: el trabajador valida el origen. Si el origen es su tienda está cubierto; si es almacén exige `TRASLADO_ORIGEN`. Falta decidir si además debe permitirse como destino el almacén sin autorización y si un trabajador autorizado al almacén puede enviar a cualquier tienda.
  - Traslado entre tiendas: no está definido. Permitir que un trabajador de origen afecte una tienda ajena contradice que el permiso de almacén no otorga otra tienda; requiere decisión explícita antes de implementarlo.
  - Conteo en almacén: permitido solo con autorización activa `CONTEO`; los detalles, consulta y finalización deben reutilizar la ubicación persistida, no el ID enviado por cliente.
  - Compra que ingresa al almacén: el contrato y entidad `Compra` solo aceptan `tiendaId` y siempre crean entrada en tienda. La política aprobada dice compra de trabajador únicamente en su tienda; para compras de administrador al almacén se requeriría una decisión de negocio y cambio posterior de contrato/esquema, no inferirlo ahora.
  - Venta con ID de otra tienda: `VentaService` debe rechazarla con `403` antes de consultar o descontar stock; el `tiendaId` de request no puede suplantar el ámbito del trabajador.
  - Reportes consolidados: para trabajador deben filtrarse internamente a su tienda, incluidos los endpoints sin parámetro de tienda. La política no concede reportes de almacén a trabajador aun si tiene autorización operativa; confirmar si esa exclusión es intencional.
  - Cambio de tienda de usuario: el alcance se evalúa contra el `Usuario.tienda` actual en cada solicitud, no se almacena en JWT. Falta decidir el tratamiento de conteos/movimientos pendientes iniciados bajo la tienda anterior y si el cambio debe requerir cierre previo de conteos activos.
- Riesgos de seguridad: colocar reglas solo en controladores deja accesos internos y operaciones posteriores sin cubrir; por ello la autorización debe residir en servicios. Filtrar listados solo desde un `tiendaId` recibido permite escalamiento por ID. Un booleano de almacén produciría sobreprivilegio y eliminaría trazabilidad. La respuesta `403` para recurso existente puede revelar su existencia, pero se ajusta a la distinción solicitada; debe mantenerse consistente. El log actual de JWT contiene PII. No existen pruebas de autorización por ubicación.
- Seguimiento de 4.3.1: el lock por producto serializa las actualizaciones de stock del mismo producto, incluso en varias ubicaciones, pero un traslado toca dos stocks y debe conservar el mismo orden de locks de producto/movimiento y validarse bajo concurrencia. La creación de stock inexistente depende del lock del producto y de la restricción única existente; falta demostrarlo en PostgreSQL. Permanecen pendientes pruebas reales en PostgreSQL aislado de ventas, aprobaciones/rechazos, traslados de múltiples ubicaciones y creación concurrente de stock; H2 y mocks no prueban esa semántica.
- Estado de la tarea: PENDIENTE DE APROBACIÓN TÉCNICA.

## Decisiones tomadas durante el desarrollo
- Se agregó un usuario de prueba con rol TRABAJADOR (variables `TEST_WORKER_EMAIL` / `TEST_WORKER_PASSWORD`) para facilitar pruebas de endpoints protegidos por rol. Este usuario no debe usarse en producción.

## Pendientes / dudas
(nada aún)
