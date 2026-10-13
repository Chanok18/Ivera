# Progreso — Ivera

## Estado actual
Sprint 4 — Tarea 4.4.3: conflicto concurrente de autorizaciones normalizado y validado

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

### Sprint 4 — Tarea 4.3.3: Implementación de autorización por ubicación
- Política implementada: `ADMINISTRADOR` opera globalmente; `TRABAJADOR` opera en su `Usuario.tienda` actual. Catálogo y stock consolidado conservan lectura global. Los permisos de almacén son independientes: `CONTEO`, `AJUSTE` y `TRASLADO_ORIGEN`. ENTRADA y SALIDA manuales quedan solo para administrador; aprobar/rechazar también se valida en servicio como operación exclusiva de administrador.
- Modelo creado: `AutorizacionAlmacenTrabajador`, `AccionAlmacen` y `EstadoAutorizacionAlmacen`. Cada registro conserva trabajador, almacén, acción, estado, administrador y fecha de otorgamiento, administrador y fecha de revocación. Se preparó `backend/src/main/resources/db/migration/V4_3_3__create_autorizacion_almacen_trabajador.sql`, aditiva y manual, con FKs e índice único parcial PostgreSQL que impide dos autorizaciones activas equivalentes. No se ejecutó en Railway.
- Servicio centralizado: `ServicioAutorizacionUbicacion` resuelve el usuario autenticado desde base de datos en cada solicitud y valida rol, tienda actual y autorización activa de almacén. Los IDs de cliente solo identifican el recurso y nunca otorgan alcance. Una revocación se consulta nuevamente al agregar detalle, finalizar, consultar o iniciar conteos, por lo que su efecto es inmediato. El cambio de `Usuario.tienda` surte efecto en la siguiente solicitud sin modificar históricos.
- Administración protegida: `AutorizacionAlmacenService` y `AutorizacionAlmacenController` agregan `GET /api/autorizaciones-almacen`, `POST /api/autorizaciones-almacen` y `PUT /api/autorizaciones-almacen/{id}/revocar`, todos restringidos a administrador. El servicio exige destinatario `TRABAJADOR`, toma otorgante/revocador del principal autenticado y conserva historial revocado. Un trabajador no puede autootorgarse permisos.
- Operaciones protegidas: ventas y compras validan tienda antes de persistir; listados y detalles se restringen a la tienda actual del trabajador. Conteos validan ubicación y permiso `CONTEO` en inicio, detalle, finalización y consulta. Ajustes validan `AJUSTE`; traslados validan solo origen con `TRASLADO_ORIGEN`, por lo que un trabajador puede solicitar desde su tienda hacia cualquier destino sin obtener permiso de destino. Reportes de trabajador consultan únicamente su tienda, incluidos stock bajo, diferencias y más vendidos; un permiso de almacén no habilita reportes de almacén.
- Respuestas de seguridad: solicitudes no autenticadas responden `401`; autenticadas sin permiso responden `403` mediante `AccessDeniedException`; recursos inexistentes mantienen `404`; conflictos de estado, histórico incompleto y concurrencia conservan `409` donde aplica.
- Pruebas agregadas/extendidas: acceso global administrador, tienda propia y ajena, permiso de almacén activo/revocado, cambio de tienda, alcance de reportes, venta y compra ajena, autootorgamiento, otorgamiento/revocación auditables, ENTRADA manual de trabajador y traslado desde origen no autorizado, además de protección MVC del recurso administrativo.
- Verificaciones: `mvn clean test` -> 46 pruebas exitosas, 0 fallidas, 0 errores; `mvn clean compile` -> `BUILD SUCCESS`; `mvn package` -> `BUILD SUCCESS` y JAR generado.
- Riesgos pendientes: la migración debe revisarse y ejecutarse manualmente en PostgreSQL antes del despliegue. Las pruebas JPA usan H2 y no verifican el índice parcial de PostgreSQL ni carreras de otorgamiento simultáneo. Continúan pendientes las pruebas reales PostgreSQL de concurrencia de 4.3.1; no se modificaron compras para recibir en almacén.
- Estado de la tarea: COMPLETADA.

### Sprint 4 — Tarea 4.4: Validación de PostgreSQL, migraciones y concurrencia
- Configuración revisada: `pom.xml` incluye el driver PostgreSQL solo en runtime y H2 solo en pruebas. No hay Flyway ni Liquibase ni código que ejecute `db/migration` automáticamente. `application.yml` obtiene URL, usuario y contraseña por variables de entorno, usa dialecto PostgreSQL y mantiene `spring.jpa.hibernate.ddl-auto: update`.
- Riesgo principal de esquema: `ddl-auto:update` permite a Hibernate alterar el esquema al iniciar, sin historial versionado. Esto puede crear columnas/tablas antes de los scripts manuales y no garantiza el índice parcial de PostgreSQL. Antes de un despliegue controlado se debe acordar cambiar producción a `ddl-auto: validate` y adoptar/definir un procedimiento de migración versionado; no se cambió esta configuración en esta tarea.
- Migración `V4_2_1__add_venta_detalle_cantidad_base.sql`: coincide con `VentaDetalle.cantidadBase` (`NUMERIC(10,2)`, nullable). `ADD COLUMN IF NOT EXISTS` preserva los históricos como `NULL`, no inventa cantidades y puede ejecutarse sobre la estructura existente mientras `venta_detalle` exista. El comentario es compatible con PostgreSQL. La reversión no es segura después de que ventas nuevas escriban `cantidad_base`; en caso de fallo se restaura respaldo, no se elimina la columna con datos.
- Migración `V4_3_3__create_autorizacion_almacen_trabajador.sql`: coincide con `AutorizacionAlmacenTrabajador`: PK `BIGSERIAL`, FKs a `usuario`/`almacen`, acciones, estado y auditoría de otorgamiento/revocación. El índice parcial único `(usuario_id, almacen_id, accion) WHERE estado = 'ACTIVA'` es sintaxis PostgreSQL válida y evita permisos activos duplicados, permitiendo conservar historial revocado. `TIMESTAMP` es compatible con `LocalDateTime`.
- Limitaciones estáticas de la segunda migración: no agrega `CHECK` para los valores permitidos de `accion`/`estado` ni para coherencia entre `estado`, `revocado_en` y `revocado_por_usuario_id`; la aplicación los mantiene, pero SQL directo podría violarlos. `CREATE TABLE IF NOT EXISTS` no repara una tabla ya existente pero incompleta. No se modificó el script porque requiere decidir si esas restricciones adicionales son parte del contrato de esquema aprobado.
- Orden y dependencia: aplicar primero `V4_2_1` y después `V4_3_3`. Son independientes entre sí, pero ambas dependen de las tablas existentes de la aplicación: `venta_detalle` para la primera y `usuario`/`almacen` para la segunda. No existe tabla de historial de migraciones, por lo que el registro de versión aplicada debe hacerse operacionalmente hasta adoptar una herramienta.
- Entorno comprobado: Docker Desktop no está activo. PostgreSQL 18 local está escuchando en `127.0.0.1:5432`, con cliente `psql` instalado y servicio Windows activo, pero la consulta de catálogo local quedó esperando autenticación sin credenciales no interactivas. No se pudo confirmar que exista una base aislada ni que no contenga datos productivos. No se usaron credenciales, no se conectó a Railway, no se aplicaron migraciones y no se ejecutaron pruebas contra PostgreSQL.
- Pruebas de integración PostgreSQL pendientes: dos ventas concurrentes; dos aprobaciones; aprobar/rechazar simultáneos; traslado concurrente con venta; creación concurrente de stock inexistente; bloqueo sobre varias ubicaciones; prevención de negativo; persistencia de `cantidadBase`; otorgamiento/revocación; e índice parcial ante permiso activo duplicado. Las pruebas actuales con H2 y mocks no validan locks, índice parcial ni semántica de PostgreSQL.
- Verificaciones locales ejecutadas: `mvn clean test` -> 46 pruebas exitosas, 0 fallidas, 0 errores; `mvn clean compile` -> `BUILD SUCCESS`; `mvn package` -> `BUILD SUCCESS` y JAR generado. Estas verificaciones no usan PostgreSQL.
- Procedimiento seguro propuesto, sin ejecutar:
  1. Crear una base PostgreSQL nueva y no productiva, por ejemplo `ivera_test`, con credenciales locales separadas y `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` apuntando solo a ella. Confirmar con `SELECT current_database(), inet_server_addr(), inet_server_port();` y revisar que no contiene tablas/datos productivos antes de usarla.
  2. Crear respaldo antes de toda migración productiva con `pg_dump --format=custom --file=ivera_pre_4_4.dump "$env:PGDATABASE"`, usando variables de entorno de una cuenta autorizada; conservar y verificar el archivo. Nunca incluir credenciales en comandos, repositorio o logs.
  3. Verificar previamente que existen `venta_detalle`, `usuario` y `almacen`; inspeccionar `information_schema.columns` para confirmar si `venta_detalle.cantidad_base` y `autorizacion_almacen_trabajador` ya existen.
  4. Aplicar en orden, con parada ante error y una transacción por script: `psql -v ON_ERROR_STOP=1 --single-transaction -f V4_2_1__add_venta_detalle_cantidad_base.sql` y después el mismo comando para `V4_3_3__create_autorizacion_almacen_trabajador.sql`.
  5. Verificar después: tipo/nullabilidad de `venta_detalle.cantidad_base`; `to_regclass('autorizacion_almacen_trabajador')`; FKs en `pg_constraint`; y definición del índice `uk_autorizacion_almacen_activa` en `pg_indexes`. Probar en la base aislada que una segunda autorización `ACTIVA` equivalente falla y que una `REVOCADA` histórica no impide una nueva activa.
  6. Ejecutar la batería de concurrencia desde dos transacciones/conexiones independientes contra PostgreSQL aislado y comprobar stock final, un solo estado final de movimiento, ausencia de negativos y conservación de `cantidad_base`.
  7. Para producción, activar mantenimiento, confirmar respaldo y destino, aplicar los scripts manualmente en el mismo orden, ejecutar verificaciones solo de lectura y pruebas de salud. Si falla un script, detener el despliegue y restaurar el respaldo; no ejecutar `DROP COLUMN`/`DROP TABLE` como recuperación automática.
- Estado real: COMPATIBILIDAD ESTÁTICA REVISADA; VALIDACIÓN DE MIGRACIONES Y CONCURRENCIA EN POSTGRESQL PENDIENTE DE ENTORNO AISLADO CONFIRMADO.

### Sprint 4 — Tarea 4.4.1: Preparación y validación de PostgreSQL local
- Configuración revisada: `application.yml` obtiene la conexión mediante `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`, con `ddl-auto: update`; `pom.xml` incluye PostgreSQL en runtime y H2 solo para pruebas. No existe perfil de integración PostgreSQL ni mecanismo automático de migraciones.
- Intento de conexión seguro: se ejecutó `psql` sin solicitar contraseña interactiva (`-w`) contra `127.0.0.1:5432`, usuario `postgres` y base explícita `ivera_test`, para consultar `current_database()` y el número de tablas. PostgreSQL rechazó la conexión con `fe_sendauth: no password supplied` antes de ejecutar SQL.
- Reintento tras configurar `pgpass.conf`: `psql -w` confirmó `current_database() = ivera_test`, `inet_server_addr() = 127.0.0.1`, puerto `5432` y `0` tablas en el esquema `public`. El aislamiento para comandos `psql` queda confirmado.
- Bloqueo de JDBC: el controlador PostgreSQL usado por Spring/Hikari no autenticó con `DB_PASSWORD` vacío y falló antes de ejecutar DDL (`SQLState 28P01`). `pgpass.conf` permitió `psql`, pero no proporcionó una contraseña a la configuración `spring.datasource.password` actual. Se usó un árbol temporal del commit `736576b`, anterior a ambas migraciones, con `DataInitializer` desactivado y `ddl-auto=create` solicitado; el arranque falló por autenticación y no creó tablas.
- Verificación posterior de variables del proceso: `DB_PASSWORD` está disponible sin inspeccionar su valor, pero `DB_URL` no coincide exactamente con `jdbc:postgresql://127.0.0.1:5432/ivera_test`. Por seguridad no se imprimió el valor recibido, no se abrió conexión JDBC y no se ejecutó DDL ni migración.
- Diagnóstico de la discrepancia: el proceso de OpenCode recibe `DB_URL=jdbc:postgresql://localhost:5432/ivera`, mientras `DB_USERNAME` y `DB_PASSWORD` están disponibles. No se encontró `.env`, perfil Spring adicional, variable `SPRING_DATASOURCE_*`, configuración Maven ni argumento de ejecución en el repositorio que lo sobrescriba; el valor proviene del entorno heredado por OpenCode. No se ejecutó la consulta JDBC de lectura porque habría apuntado a la base `ivera`, prohibida para esta tarea.
- Seguridad preservada: no se conectó a Railway, `ivera` ni `syncria_db`; no se escribió ni mostró contraseña, no se creó ni alteró esquema, no se aplicaron migraciones y no se ejecutaron pruebas PostgreSQL.
- Bloqueo: corregir `DB_URL` en el entorno de OpenCode para que coincida exactamente con `jdbc:postgresql://127.0.0.1:5432/ivera_test`; `DB_PASSWORD` ya está disponible. Tras corregirla se debe reintentar la conexión JDBC de solo lectura y confirmar el destino antes de cualquier DDL.
- Estado real: AISLAMIENTO `psql` CONFIRMADO; BLOQUEADA PENDIENTE DE `DB_URL` JDBC EXCLUSIVA PARA `ivera_test`.

### Sprint 4 — Tarea 4.4.1: Revalidación JDBC local
- Variables verificadas sin exponer secretos: `DB_URL` coincide exactamente con `jdbc:postgresql://127.0.0.1:5432/ivera_test` y `DB_PASSWORD` está disponible en el proceso.
- Conexión ejecutada exclusivamente por JDBC, sin iniciar Spring ni Hibernate. La conexión abrió una transacción con `Connection.setReadOnly(true)` y `SET TRANSACTION READ ONLY`; al finalizar se hizo `rollback`.
- Consulta ejecutada: `SELECT current_database(), inet_server_addr(), inet_server_port();`. Resultado: base `ivera_test`, servidor `127.0.0.1`, puerto `5432`.
- Esquema comprobado con consulta de solo lectura al catálogo PostgreSQL: `public` contiene `0` objetos. No se ejecutó DDL, migración ni se alteró dato alguno.
- Seguridad: no se conectó a Railway, `ivera` ni `syncria_db`; no se mostraron credenciales.
- Verificación de compilación: `mvn clean compile` -> `BUILD SUCCESS` (98 archivos fuente compilados).
- Estado real: AUTENTICACIÓN JDBC Y AISLAMIENTO CONFIRMADOS; pendiente autorización para preparar el esquema histórico.

### Sprint 4 — Tarea 4.4.1: Preparación del esquema histórico local
- Commit histórico utilizado: `736576bc21f3928c93e5c3d2d64be8d8f999bed6` (`Sprint 3 completo`). Es ancestro de `HEAD`; contiene las 15 entidades base, incluida `VentaDetalle` sin `cantidadBase`, y no contiene la entidad ni la tabla `AutorizacionAlmacenTrabajador`. El diff hasta `HEAD` introduce exactamente `cantidad_base`, la entidad de autorización y ambas migraciones manuales.
- Migraciones inspeccionadas, no aplicadas: `V4_2_1__add_venta_detalle_cantidad_base.sql` agrega `venta_detalle.cantidad_base NUMERIC(10,2)` nullable; `V4_3_3__create_autorizacion_almacen_trabajador.sql` crea la tabla de autorizaciones y su índice parcial único.
- Verificación previa por JDBC de solo lectura: `DB_URL` coincidió exactamente con `jdbc:postgresql://127.0.0.1:5432/ivera_test`; `current_database() = ivera_test`; `inet_server_addr() = 127.0.0.1`; `inet_server_port() = 5432`; y `public` tenía 0 objetos.
- Método de generación: worktree temporal aislado en `C:\Users\user\AppData\Local\Temp\opencode\ivera-historico-736576b`, creado desde el commit histórico. Solo en ese árbol temporal, `DataInitializer` se restringió con el perfil `!schema-historico`. Se construyó con `mvn clean package -DskipTests` y se inició con perfil `schema-historico`, aplicación web desactivada y `ddl-auto=create`, con las variables de datasource heredadas exclusivamente hacia `ivera_test`. No se modificó `application.yml` principal ni se cargaron datos de demostración.
- Tablas creadas en `public`: `almacen`, `compra`, `compra_detalle`, `conteo_detalle`, `conteo_inventario`, `movimiento`, `producto`, `producto_presentacion`, `proveedor`, `stock`, `tienda`, `unidad_medida`, `usuario`, `venta` y `venta_detalle`. Las 15 quedaron con 0 filas.
- Verificaciones posteriores por JDBC de solo lectura: `venta_detalle` existe y no contiene `cantidad_base`; `autorizacion_almacen_trabajador` no existe. `venta_detalle` conserva FKs a `producto(id)`, `unidad_medida(id)` y `venta(id)`; `usuario(id)` y `almacen(id)` tienen sus claves primarias requeridas por la migración 4.3.3.
- Problemas e incompatibilidades: no se detectó incompatibilidad para aplicar las migraciones. Hibernate emitió avisos esperados al intentar eliminar condicionalmente tablas y restricciones inexistentes antes de `create`; la comprobación previa había confirmado que `public` estaba vacío, por lo que no se eliminó ningún objeto existente. No se ejecutó `DROP` sobre ninguna otra base.
- Seguridad y límites respetados: no se aplicaron migraciones ni pruebas de concurrencia; no se conectó a Railway, `ivera` ni `syncria_db`; no se expusieron credenciales, no hubo commit ni push.
- Verificación de compilación del árbol principal: `mvn clean compile` -> `BUILD SUCCESS` (98 archivos fuente compilados).
- Estado real: ESQUEMA HISTÓRICO BASE PREPARADO Y VERIFICADO EN `ivera_test`; pendiente autorización para aplicar las migraciones 4.2.1 y 4.3.3.

### Sprint 4 — Tarea 4.4.1: Aplicación y validación de migraciones locales
- Seguridad previa por JDBC: `DB_URL` coincidió exactamente con `jdbc:postgresql://127.0.0.1:5432/ivera_test`; `current_database() = ivera_test`; `inet_server_addr() = 127.0.0.1`; `inet_server_port() = 5432`. Se confirmaron las 15 tablas históricas y la ausencia de `venta_detalle.cantidad_base` y de `autorizacion_almacen_trabajador` antes de todo DDL.
- Orden y método de ejecución: se aplicaron manualmente mediante `psql -X -w -v ON_ERROR_STOP=1 --single-transaction`, exclusivamente contra `127.0.0.1:5432/ivera_test`, primero `V4_2_1__add_venta_detalle_cantidad_base.sql` y luego `V4_3_3__create_autorizacion_almacen_trabajador.sql`.
- Resultado de `V4_2_1`: ejecutó `ALTER TABLE` y `COMMENT`. `venta_detalle.cantidad_base` existe como `NUMERIC(10,2)`, admite `NULL` para históricos y conserva el comentario del script. Las seis columnas históricas continúan presentes con sus tipos y nulabilidad; permanecen sus FKs a `producto(id)`, `unidad_medida(id)` y `venta(id)`.
- Resultado de `V4_3_3`: ejecutó `CREATE TABLE` y `CREATE INDEX`. `autorizacion_almacen_trabajador` tiene las 9 columnas compatibles con la entidad JPA: `id BIGINT` con secuencia, referencias `usuario_id`/`almacen_id`/`otorgado_por_usuario_id` obligatorias, `accion VARCHAR(30)`, `estado VARCHAR(15)`, `otorgado_en TIMESTAMP` obligatoria y referencias/fecha de revocación nullable. Se verificaron la PK y 4 FKs: `usuario_id -> usuario(id)`, `almacen_id -> almacen(id)`, `otorgado_por_usuario_id -> usuario(id)` y `revocado_por_usuario_id -> usuario(id)`.
- Índice y restricciones: existe `uk_autorizacion_almacen_activa` como índice único btree sobre `(usuario_id, almacen_id, accion)` con predicado `WHERE estado = 'ACTIVA'`. Los campos obligatorios del script son `NOT NULL`, la PK y las FKs están activas. El script no declara restricciones `CHECK` de dominio o coherencia de revocación; esa limitación permanece conocida y no se modificó el SQL.
- Integridad final por JDBC de solo lectura: existen exactamente 16 tablas: las 15 históricas más `autorizacion_almacen_trabajador`; no hubo pérdida de tablas. `venta_detalle` tiene 7 columnas y la nueva tabla 9; no se detectó pérdida de columnas de las tablas afectadas.
- Compatibilidad JPA realmente comprobada: se construyó un worktree temporal del `HEAD`, se desactivó `DataInitializer` solo en ese árbol temporal mediante el perfil `schema-validation`, se desactivó la aplicación web y se inició contra `ivera_test` con `spring.jpa.hibernate.ddl-auto=validate`. Hibernate inicializó correctamente el `EntityManagerFactory` y los 16 repositorios sin error de validación ni DDL. El `HEAD` comprometido tenía un import faltante de `AccionAlmacen` en `ConteoService`; se replicó solo dentro del worktree temporal el import que ya existe en el árbol de trabajo actual para poder compilar. No cambió ningún mapeo JPA ni archivo del proyecto principal.
- Verificación de compilación del árbol principal: `mvn clean compile` -> `BUILD SUCCESS` (98 archivos fuente compilados).
- Problemas y riesgos pendientes: no se ejecutaron pruebas de concurrencia. Sigue pendiente validar en PostgreSQL real las carreras de ventas, aprobaciones/rechazos, traslados y creación concurrente de stock de la tarea 4.3.1. No se insertaron datos para provocar una violación del índice parcial; su existencia y predicado se comprobaron por catálogo.
- Seguridad y límites respetados: no se conectó a Railway, `ivera` ni `syncria_db`; no se expusieron credenciales, no se modificaron contratos públicos, no hubo commit ni push. El worktree temporal de validación se eliminó tras finalizar.
- Estado real: MIGRACIONES 4.2.1 Y 4.3.3 APLICADAS Y VALIDADAS EN `ivera_test`; ESQUEMA COMPATIBLE CON JPA MEDIANTE `ddl-auto=validate`; PENDIENTE AUTORIZACIÓN PARA PRUEBAS DE CONCURRENCIA POSTGRESQL.

### Sprint 4 — Tarea 4.4.2: Pruebas reales de concurrencia PostgreSQL
- Seguridad previa: se verificó por JDBC que `DB_URL` coincide exactamente con `jdbc:postgresql://127.0.0.1:5432/ivera_test`, y que el destino respondió `ivera_test`, `127.0.0.1`, `5432`, con 16 tablas, `venta_detalle.cantidad_base` y `autorizacion_almacen_trabajador` presentes. No se conectó a Railway, `ivera` ni `syncria_db`.
- Infraestructura agregada: perfil de prueba `postgres-integration` en `src/test/resources/application-postgres-integration.yml`, con URL local explícita y `spring.jpa.hibernate.ddl-auto=validate`. `DataInitializer` queda excluido únicamente bajo dicho perfil mediante `@Profile("!postgres-integration")`. La suite solo se habilita con `RUN_POSTGRES_INTEGRATION=true`, para no conectarse accidentalmente en ejecuciones ordinarias.
- Suite real creada: `PostgresConcurrencyTest` usa `@SpringBootTest`, PostgreSQL local real, servicios transaccionales reales, dos hilos por carrera, `CountDownLatch` para inicio coordinado, `Future.get` con timeout de 15 segundos, timeout global de 30 segundos, y `SecurityContext` independiente por hilo. No usa H2, mocks ni transacción de prueba envolvente. Los fixtures llevan prefijo UUID y se eliminan selectivamente respetando FKs; tras ejecutar se verificó por JDBC que quedaron 0 usuarios y 0 productos `pgit-*`.
- Escenario 1, dos ventas simultáneas (2 operaciones): PASS. Stock inicial 10, dos solicitudes de 8; una venta confirmó y la otra recibió `IllegalArgumentException` por stock insuficiente. Stock final verificado: 2; una venta y un movimiento `SALIDA`; sin negativo.
- Escenario 2, dos aprobaciones simultáneas (2 operaciones): PASS. Un traslado pendiente de 5 fue aprobado una sola vez; el segundo actor recibió `IllegalStateException`. Estado final `APROBADO`; stock origen 5 y destino 5.
- Escenario 3, aprobación y rechazo simultáneos (2 operaciones): PASS. Solo una transición consumió `PENDIENTE`; el perdedor recibió `IllegalStateException`. La prueba verificó el estado persistido y el stock coherente: si aprobó, origen 5/destino 5; si rechazó, origen 10/sin stock destino.
- Escenario 4, venta contra aprobación de traslado (2 operaciones): PASS. Ambas operaciones compitieron por el mismo producto y stock inicial 10. En la ejecución realizada ganó el traslado: origen 2, destino 8 y la venta falló por insuficiencia. La prueba también admite el orden inverso, pero siempre verifica ausencia de negativos y conservación del total aplicable.
- Escenario 5, stock inexistente (2 operaciones): PASS. Dos `ENTRADA` simultáneas de 3 y 4 sobre una ubicación sin fila de stock crearon exactamente una fila, con cantidad final 7; ambas operaciones confirmaron.
- Escenario 6, múltiples ubicaciones (2 operaciones): PASS. Se aprobaron en paralelo traslados opuestos entre dos tiendas para el mismo producto. No hubo deadlock ni timeout; stock final origen 11 y destino 9. El bloqueo global por producto serializó los cambios.
- Escenario 7, cantidad base: PASS. Una venta de una presentación con factor 12 persistió `VentaDetalle.cantidadBase = 12`; cambiar después el factor a 24 no modificó el histórico.
- Escenario 8, autorizaciones de almacén (2 otorgamientos concurrentes más verificaciones secuenciales): PASS de integridad. Solo quedó una autorización `ACTIVA`; el segundo otorgamiento produjo en PostgreSQL la violación real `SQLState 23505` del índice parcial `uk_autorizacion_almacen_activa`. Con el permiso activo, el trabajador pudo registrar un ajuste de almacén; tras revocar, el mismo flujo recibió `AccessDeniedException`.
- Defecto no crítico detectado: la carrera de otorgamiento se protege correctamente en la base mediante el índice parcial, pero el segundo actor recibe `DataIntegrityViolationException` técnica en vez de un conflicto de negocio normalizado. No se modificó código productivo para corregirlo, pendiente de autorización.
- Riesgos pendientes: las pruebas se ejecutaron una vez por escenario, no como stress test repetido; no se validaron timeouts configurados por PostgreSQL porque no se produjo deadlock. El bloqueo por producto protege integridad, pero serializa operaciones del mismo producto incluso entre ubicaciones distintas. Esta validación local no certifica preparación para producción.
- Ejecuciones reales: `mvn clean test -Dtest=PostgresConcurrencyTest` con `RUN_POSTGRES_INTEGRATION=true` -> 8 pruebas PostgreSQL exitosas. `mvn clean test` con la misma variable -> 54 pruebas exitosas, 0 fallidas, 0 errores, 0 omitidas (46 H2/mocks + 8 PostgreSQL). `mvn clean compile` -> `BUILD SUCCESS`. `mvn package` posterior -> `BUILD SUCCESS`; en esa ejecución sin la variable explícita las 8 pruebas PostgreSQL quedaron omitidas intencionalmente y las 46 restantes pasaron.
- Estado real: CONCURRENCIA E INTEGRIDAD PRINCIPALES VALIDADAS CONTRA POSTGRESQL 18 LOCAL EN `ivera_test`; NO SE EJECUTARON CAMBIOS DE REGLAS DE NEGOCIO, MIGRACIONES, FRONTEND, COMMIT NI PUSH.

### Sprint 4 — Tarea 4.4.3: Normalización de autorización activa duplicada
- Seguridad previa: antes de ejecutar las pruebas de escritura se confirmó que `DB_URL` coincide exactamente con `jdbc:postgresql://127.0.0.1:5432/ivera_test`; JDBC confirmó `current_database() = ivera_test`, `inet_server_addr() = 127.0.0.1`, `inet_server_port() = 5432` y 16 tablas. No se conectó a Railway, `ivera` ni `syncria_db`.
- Causa raíz: `existsByTrabajadorIdAndAlmacenIdAndAccionAndEstado(...)` evita duplicados secuenciales, pero dos transacciones pueden superar esa lectura antes de insertar. PostgreSQL conserva la integridad con el índice único parcial `uk_autorizacion_almacen_activa` sobre `(usuario_id, almacen_id, accion) WHERE estado = 'ACTIVA'`; la transacción perdedora termina con `DataIntegrityViolationException`, incluso si la detección se produce al commit.
- Corrección aplicada: `GlobalExceptionHandler` inspecciona la cadena causal de `DataIntegrityViolationException`. Solo cuando encuentra un `SQLException` con `SQLState 23505` y la restricción exacta `uk_autorizacion_almacen_activa` responde HTTP `409 Conflict` con el formato existente `{"error":"Ya existe una autorización activa para este trabajador, almacén y acción"}`. El nombre interno de la restricción y los detalles SQL nunca se incluyen en la respuesta. Cualquier otra violación de integridad responde `500` con `{"error":"No se pudo completar la operación."}`, por lo que no se clasifica indiscriminadamente como conflicto de autorización.
- Diseño transaccional: no se capturó la excepción dentro de `AutorizacionAlmacenService`, porque PostgreSQL/JPA puede lanzarla al hacer flush o durante el commit, fuera del cuerpo del método. El índice parcial se conserva sin modificación y sigue siendo la protección definitiva ante la carrera.
- Pruebas HTTP agregadas: `AutorizacionAlmacenControllerTest` usa MockMvc para comprobar que un `DataIntegrityViolationException` con causa `23505` de la restricción autorizada devuelve `409` y el cuerpo seguro esperado; otra restricción única simulada devuelve `500` genérico y no se anuncia como autorización duplicada. Resultado: 4 pruebas del controlador exitosas.
- Pruebas PostgreSQL ejecutadas: `PostgresConcurrencyTest` volvió a ejecutar los 8 escenarios reales contra `ivera_test`; el escenario de dos otorgamientos concurrentes produjo nuevamente la violación real `23505`, dejó exactamente una autorización activa y pasó. La normalización HTTP se verificó por separado en MockMvc, que reproduce la misma forma de excepción traducida por Spring.
- Ejecuciones reales: `mvn clean test -Dtest=AutorizacionAlmacenControllerTest` -> 4 exitosas. `mvn clean test -Dtest=PostgresConcurrencyTest` con `RUN_POSTGRES_INTEGRATION=true` -> 8 exitosas. `mvn clean test` con `RUN_POSTGRES_INTEGRATION=true` -> 56 exitosas, 0 fallidas, 0 errores, 0 omitidas. `mvn clean compile` -> `BUILD SUCCESS`. `mvn package` -> `BUILD SUCCESS`; al no habilitar la variable en esa ejecución, las 8 pruebas PostgreSQL quedaron omitidas intencionalmente y las 48 restantes pasaron.
- Riesgos pendientes: la normalización específica depende del `SQLState 23505` y del nombre estable de la restricción ya versionada; un renombrado futuro debe actualizar esta detección y sus pruebas. No se hizo prueba HTTP concurrente contra un servidor desplegado: la carrera real se ejecutó a nivel servicio/PostgreSQL y el contrato HTTP se verificó mediante MockMvc. No se realizaron cambios de migración, contratos públicos, reglas de autorización, frontend, commit ni push.
- Estado real: CONFLICTO DE AUTORIZACIÓN ACTIVA DUPLICADA NORMALIZADO A HTTP 409 SOLO PARA LA RESTRICCIÓN CONOCIDA; ÍNDICE PARCIAL E INTEGRIDAD POSTGRESQL CONSERVADOS Y VALIDADOS.

## Decisiones tomadas durante el desarrollo
- Se agregó un usuario de prueba con rol TRABAJADOR (variables `TEST_WORKER_EMAIL` / `TEST_WORKER_PASSWORD`) para facilitar pruebas de endpoints protegidos por rol. Este usuario no debe usarse en producción.

## Pendientes / dudas
(nada aún)
