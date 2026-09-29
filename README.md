# Documentación técnica — Sistema de inventario en la nube "TodoPernos"

> Documento base del proyecto. Úsalo como contexto fijo para los agentes de OpenCode y como insumo para el informe de tesis. Cualquier decisión que no esté aquí NO debe asumirse por el agente sin confirmarla contigo primero.

---

## 1. Visión del proyecto

Sistema web y app móvil en la nube para centralizar el inventario de una ferretería con 2 tiendas ("Romel" y "Todopernos", cada una con RUC propio) que comparten un almacén central. Hoy la información vive separada en cada tienda (sistema local) y en un sistema en la nube usado solo para facturación — ninguno está centralizado. El diferenciador del proyecto es el **inventario móvil por código de barras**, con actualización de stock en tiempo casi real entre ambas tiendas y el almacén.

**Fuera de alcance (asunción a confirmar contigo):** este sistema NO reemplaza la facturación electrónica (boletas/facturas/guías de remisión) que ya manejan en su sistema actual en la nube. El nuevo sistema registra ventas y compras a nivel interno (para descontar/sumar stock y dar trazabilidad), pero no emite comprobantes electrónicos ante SUNAT. Si esto cambia, hay que actualizar el modelo de datos antes de tocar código.

---

## 2. Stack tecnológico (definitivo, no cambiar sin confirmar)

| Capa | Tecnología |
|---|---|
| Frontend web | React + TypeScript + Vite |
| App móvil | Kotlin + Jetpack Compose + CameraX + ML Kit (escaneo de código de barras) |
| Backend | Java 21 + Spring Boot + Spring Data JPA |
| Base de datos | PostgreSQL en la nube (Railway) |
| Seguridad | Spring Security + JWT |
| Documentación API | OpenAPI/Swagger |
| Hosting backend + BD | Railway |
| Hosting frontend web | Vercel |
| Imágenes de producto | Cloudinary o AWS S3 |
| Control de versiones | Git + GitHub |
| Contenedores | Docker |
| Diseño UI | Figma / Figma Make (paleta: naranja + negro, fondo blanco hueso) |
| Pruebas de API | Postman |

**Reglas para los agentes:** no proponer otra base de datos, otro framework backend/frontend, ni otro proveedor de hosting sin que Kevin lo apruebe explícitamente.

---

## 3. Roles y permisos

Solo 2 roles — **no crear roles adicionales**:

- **ADMINISTRADOR** (1 por tienda): acceso total — productos, inventario, movimientos, aprobaciones, usuarios, reportes, configuración.
- **TRABAJADOR** (compartido entre quien vende y quien hace almacén, sin distinción): puede ver productos, registrar ventas, registrar movimientos de inventario (entradas/salidas/ajustes/conteos) y compras — pero ciertas acciones quedan en estado `PENDIENTE` hasta que un Administrador las apruebe.

**Implementación recomendada:** permisos por acción/módulo asociados a cada rol (no roles hardcodeados en la lógica de negocio), para poder ajustar sin rediseñar si el negocio lo pide más adelante.

**Acciones que requieren aprobación del Administrador (a implementar con estado `PENDIENTE`/`APROBADO`/`RECHAZADO`):**
- Ajustes de inventario por diferencia detectada en conteo.
- Traslados de stock entre tienda y almacén central.

---

## 4. Modelo de datos

### 4.1 Entidades principales

**Tienda**
- id (PK)
- nombre (ej. "Romel", "Todopernos")
- ruc
- direccion

**Almacen**
- id (PK)
- nombre (ej. "Almacén Central")

**Usuario**
- id (PK)
- nombre
- email (único)
- password_hash
- rol (`ADMINISTRADOR` | `TRABAJADOR`)
- tienda_id (FK → Tienda, nullable — a qué tienda pertenece principalmente)

**UnidadMedida**
- id (PK)
- nombre (ej. "unidad", "docena", "centena", "millar", "saco", "metro")

**Producto**
- id (PK)
- nombre
- descripcion
- categoria
- codigo_barras (nullable — no todos lo tienen aún)
- unidad_base_id (FK → UnidadMedida — la unidad mínima de conteo)
- imagen_url (nullable)
- precio_unitario (precio en la unidad base)

**ProductoPresentacion** (resuelve el problema de saco/metro/unidad)
- id (PK)
- producto_id (FK → Producto)
- unidad_id (FK → UnidadMedida)
- factor_conversion (cuántas unidades base contiene, ej. 1 saco = 25 unidades base)
- precio_presentacion (nullable, precio de venta en esa presentación)

**Stock**
- id (PK)
- producto_id (FK → Producto)
- ubicacion_tipo (`TIENDA` | `ALMACEN`)
- ubicacion_id (id de Tienda o Almacen según el tipo)
- cantidad (siempre expresada en unidad base)

**Movimiento**
- id (PK)
- producto_id (FK → Producto)
- tipo (`ENTRADA` | `SALIDA` | `AJUSTE` | `TRASLADO`)
- cantidad (en unidad base)
- unidad_id (FK → UnidadMedida, la unidad en que se registró originalmente)
- ubicacion_origen_tipo / ubicacion_origen_id
- ubicacion_destino_tipo / ubicacion_destino_id (nullable, solo para traslados)
- usuario_id (FK → Usuario, quien lo registró)
- estado (`PENDIENTE` | `APROBADO` | `RECHAZADO`)
- aprobado_por (FK → Usuario, nullable)
- fecha
- motivo (texto libre, ej. "diferencia detectada en conteo")

**Proveedor**
- id (PK)
- nombre
- ruc
- contacto

**Compra**
- id (PK)
- proveedor_id (FK → Proveedor)
- tienda_id (FK → Tienda)
- usuario_id (FK → Usuario)
- fecha
- total
- estado (`REGISTRADA` | `RECIBIDA`)

**CompraDetalle**
- id (PK)
- compra_id (FK → Compra)
- producto_id (FK → Producto)
- cantidad
- unidad_id (FK → UnidadMedida)
- precio_unitario

**Venta** (registro interno, no reemplaza facturación electrónica)
- id (PK)
- tienda_id (FK → Tienda)
- usuario_id (FK → Usuario)
- fecha
- total

**VentaDetalle**
- id (PK)
- venta_id (FK → Venta)
- producto_id (FK → Producto)
- cantidad
- unidad_id (FK → UnidadMedida)
- precio_unitario

**ConteoInventario**
- id (PK)
- ubicacion_tipo / ubicacion_id
- usuario_id (FK → Usuario)
- fecha
- estado (`EN_PROCESO` | `FINALIZADO`)

**ConteoDetalle**
- id (PK)
- conteo_id (FK → ConteoInventario)
- producto_id (FK → Producto)
- cantidad_contada
- cantidad_sistema
- diferencia (calculada: cantidad_contada - cantidad_sistema)

### 4.2 Reglas clave del modelo

1. **Todo el stock se guarda en unidad base.** Las presentaciones (saco, docena, etc.) son solo para la interfaz y el registro de movimientos; la conversión ocurre al guardar.
2. **El stock es por ubicación** (tienda o almacén), pero el catálogo de productos es único y compartido entre ambas tiendas.
3. **Todo movimiento queda trazado**: quién, cuándo, qué producto, cuánto, y su estado de aprobación.
4. Cuando se escanea un código de barras para una venta o compra, el sistema debe generar automáticamente el `Movimiento` correspondiente (sin pasos manuales adicionales).

---

## 5. Requisitos funcionales (resumen)

- RF01: Login con JWT y rol diferenciado.
- RF02: CRUD de productos con presentaciones/unidades y su conversión.
- RF03: Búsqueda de producto mostrando de inmediato el contenido de cada presentación.
- RF04: Registro de entradas, salidas, ajustes y traslados de stock.
- RF05: Aprobación de movimientos sensibles por el Administrador.
- RF06: Conteo físico desde la app móvil, comparado contra el stock del sistema.
- RF07: Escaneo de código de barras (móvil) para identificar producto y registrar movimiento automáticamente.
- RF08: Registro de compras (con proveedor) y ventas internas.
- RF09: Reportes de stock, diferencias de inventario y movimientos por tienda.

## 6. Requisitos no funcionales (resumen)

- RNF01: Comunicación siempre por HTTPS.
- RNF02: Contraseñas con hash (BCrypt).
- RNF03: Actualización de stock visible entre tiendas en segundos, no minutos.
- RNF04: Modelo de datos preparado para agregar más tiendas a futuro sin rediseño mayor.

---

## 7. Plan de sprints (4 sprints)

### Sprint 1 — Fundaciones

**Backend**
- Configurar proyecto Spring Boot 21 y conexión a PostgreSQL (Railway).
- Crear entidades: Tienda, Almacen, Usuario, UnidadMedida, Producto, ProductoPresentacion.
- Implementar autenticación JWT + Spring Security con roles ADMINISTRADOR/TRABAJADOR.
- Endpoints CRUD de Producto (con sus presentaciones) y de login.

**Frontend web**
- Setup del proyecto (React + Vite + TS) siguiendo el diseño de Figma.
- Pantalla de login.
- Pantalla de gestión de productos (listar, crear, editar, con presentaciones).

**App móvil**
- Setup del proyecto Kotlin + Jetpack Compose.
- Pantalla de login.

**Infraestructura**
- Desplegar backend y base de datos en Railway; frontend en Vercel.
- Configurar variables de entorno para credenciales (nunca hardcodeadas).

### Sprint 2 — Inventario centralizado

**Backend**
- Entidades Stock y Movimiento.
- Endpoints para registrar entrada/salida/ajuste/traslado, con estado pendiente/aprobado.
- Lógica de actualización de stock consolidado entre tiendas y almacén.
- Endpoint de aprobación de movimientos (solo Administrador).

**Frontend web**
- Pantalla de inventario consolidado (stock por tienda y almacén).
- Pantalla de movimientos, con filtro por estado y flujo de aprobación.

**App móvil**
- Pantalla de conteo físico (ConteoInventario/ConteoDetalle).
- Registro de movimiento básico desde el móvil.

### Sprint 3 — Código de barras y automatización

**Backend**
- Entidades Proveedor, Compra, CompraDetalle, Venta, VentaDetalle.
- Endpoint de búsqueda de producto por código de barras.
- Lógica de descuento/incremento automático de stock al confirmar venta/compra escaneada.

**App móvil**
- Integración CameraX + ML Kit para escaneo de código de barras.
- Flujo de venta/compra rápida por escaneo.

**Frontend web**
- Pantallas de ventas y compras (con búsqueda o lector USB si aplica).
- Mostrar la presentación/contenido del producto directamente en la búsqueda.

### Sprint 4 — Reportes, permisos finos y cierre

**Backend**
- Endpoints de reportes: stock actual, diferencias de inventario, movimientos por tienda, productos más vendidos.
- Refinar reglas de aprobación y permisos.
- Pruebas de integración.

**Frontend web y móvil**
- Pantallas de reportes.
- Pulido de UI según el prototipo de Figma.
- Pruebas end-to-end y preparación de la demo para la sustentación.

---

## 8. Funciones de IA — "Iverita"

Asistente de IA con nombre propio, pensado como **mejora secundaria** (no reemplaza ni retrasa las funciones principales de inventario) — útil además para cubrir el pedido del asesor/jurado de incluir algo de IA en la tesis. Tiene dos usos concretos:

### 8.1 Recomendador de productos en la venta

- Cuando el Trabajador o Administrador está registrando una venta, Iverita sugiere productos complementarios para ofrecerle al cliente (ej. si vende tornillos, sugiere el tipo de broca compatible o tuercas relacionadas).
- **Enfoque recomendado para el MVP (simple y viable en el tiempo de una tesis):** reglas basadas en categoría del producto y en productos que históricamente se han vendido juntos (co-ocurrencia en `VentaDetalle`), no un modelo de machine learning complejo. Esto es suficiente para demostrar el valor y es defendible técnicamente en la sustentación.
- **Entidad adicional sugerida:** `ProductoRelacionado` (producto_id, producto_relacionado_id, tipo: "categoria" | "co-compra") — puede poblarse automáticamente con una consulta SQL de co-ocurrencia, sin necesidad de un modelo de IA entrenado aparte.
- Si más adelante se quiere un motor más sofisticado (embeddings, LLM), se puede envolver esta misma lógica detrás de un endpoint `/api/iverita/recomendaciones/{productoId}` para no acoplar el frontend a la implementación interna.

### 8.2 Búsqueda en lenguaje natural para trabajadores

- En vez de buscar el producto por su nombre exacto, el Trabajador puede escribir en lenguaje común (ej. "el tornillo grueso para madera") y el sistema interpreta la intención y devuelve productos candidatos.
- **Enfoque recomendado para el MVP:** usar un LLM (ej. la API de Claude o similar) que reciba la consulta del usuario junto con la lista de nombres/categorías/descripciones de productos (o un subconjunto relevante) y devuelva los IDs de los productos más probables, en vez de construir un motor de búsqueda semántico propio desde cero — mucho más viable en el tiempo de un sprint.
- Este endpoint (`/api/iverita/buscar`) es aparte del buscador exacto por nombre/código, que sigue existiendo para las búsquedas normales.

**Dónde ubicarlo en el plan de sprints:** ambas funciones de Iverita se agregan como parte del **Sprint 4**, después de que el flujo de ventas y el catálogo ya estén sólidos (Sprints 1-3). No debe implementarse antes, porque depende de tener datos reales de ventas y del catálogo completo.

---

## 9. Guía de diseño frontend (web y consistencia con Figma)

Esta guía traduce el diseño ya aprobado en Figma a lineamientos concretos, para que cualquier agente o desarrollador mantenga la misma identidad visual en toda la web.

### 9.1 Paleta de colores

| Uso | Color | Hex aproximado |
|---|---|---|
| Color primario (botones, acentos, iconos activos) | Naranja | `#F5761A` |
| Fondo del sidebar de navegación | Negro / gris muy oscuro | `#1A1A1A` |
| Fondo general de la página | Blanco hueso | `#FAF7F2` |
| Tarjetas / paneles | Blanco | `#FFFFFF` |
| Estado positivo (activo, completado) | Verde | `#22C55E` |
| Estado de alerta (stock bajo, en revisión) | Naranja claro / ámbar | `#F59E0B` |
| Estado crítico | Rojo | `#EF4444` |
| Texto principal | Gris oscuro / negro | `#1F2937` |
| Texto secundario | Gris medio | `#6B7280` |

### 9.2 Tipografía y estilo

- Fuente sans-serif limpia (ej. Inter, o la que traiga por defecto el sistema de diseño elegido).
- Jerarquía clara: títulos de sección en negrita, texto de tabla en peso regular.

### 9.3 Componentes y patrones

- **Sidebar:** fondo oscuro fijo a la izquierda, ítem activo resaltado en naranja, íconos + texto.
- **Tarjetas:** esquinas redondeadas (`border-radius` ~12-16px), sombra suave, fondo blanco sobre el fondo hueso general.
- **Botones primarios:** naranja sólido, texto blanco, esquinas redondeadas.
- **Badges de estado:** pastilla redondeada con color de fondo suave (verde/naranja/rojo claro) y texto del mismo tono más oscuro.
- **Selector de tienda:** visible en la barra superior (Romel / Todopernos / Almacén central), siempre indicando el contexto de datos que se está viendo.
- **Gráficos:** barras y líneas en tono naranja, consistentes con la identidad de marca.
- **Iverita (asistente de IA):** representarlo como un ícono/burbuja flotante o un campo de búsqueda especial, diferenciado visualmente del buscador exacto (por ejemplo, con un ícono distintivo y un placeholder tipo "Pregúntale a Iverita...").

**Regla para los agentes:** cualquier pantalla nueva debe reutilizar estos mismos colores y patrones — no introducir una paleta o estilo de componente distinto al ya validado en Figma.

---

## 10. Reglas para los agentes de OpenCode

1. No agregar roles, tablas, endpoints ni dependencias que no estén en este documento sin confirmarlo antes con Kevin.
2. No cambiar el stack tecnológico definido en la sección 2.
3. Todo movimiento de stock debe pasar por la entidad `Movimiento` — no modificar `Stock` directamente desde otro flujo.
4. Respetar el alcance de cada sprint; no adelantar funcionalidades de sprints posteriores sin indicación explícita. Las funciones de Iverita (sección 8) son exclusivas del Sprint 4.
5. Las cantidades siempre se calculan y guardan en unidad base, usando `ProductoPresentacion.factor_conversion` para las conversiones.
6. Mantener siempre la paleta y los patrones visuales de la sección 9 — no introducir estilos nuevos sin aprobación.

---

## 9. Funcionalidades con IA — "Iverita"

Asistente de IA del sistema, disponible tanto para Administrador como para Trabajador. Cubre 2 funciones concretas (no un chatbot general):

### 9.1 Recomendaciones durante la venta

Cuando el Trabajador o Administrador está armando una venta (agregando productos al carrito), Iverita sugiere productos complementarios para ofrecerle al cliente (ej. si agrega tornillos, sugiere brocas o tarugos relacionados).

**Cómo implementarlo (2 niveles, elige según el tiempo disponible):**
- **Nivel simple (recomendado para el MVP de tesis):** tabla `ProductoRelacionado` (producto_id, producto_relacionado_id, motivo) que se llena manualmente o a partir de qué productos se venden juntos con frecuencia (consulta SQL sobre `VentaDetalle` agrupando por venta_id). Es rápido de implementar, demostrable y no depende de un servicio externo.
- **Nivel avanzado (si quieres mostrar uso real de IA generativa):** endpoint que, al agregar un producto al carrito, llama a una API de LLM (ej. la misma que usas en OpenCode) enviando el producto actual y el catálogo relevante, y recibe 2-3 sugerencias con una breve razón. Requiere manejar la llamada externa, tiempo de respuesta y una API key configurada como variable de entorno.

### 9.2 Búsqueda en lenguaje natural

El Trabajador busca productos con lenguaje común en vez del nombre exacto del catálogo (ej. escribe "el tornillo negro grande para madera" y el sistema debe encontrar el producto correcto, aunque el nombre en el catálogo sea distinto).

**Cómo implementarlo:**
- Endpoint de búsqueda que recibe el texto libre, lo envía junto con una lista acotada de productos candidatos (filtrados primero por coincidencia parcial de texto o categoría) a una API de LLM, y el modelo devuelve cuál(es) coinciden mejor con la intención del usuario.
- Alternativa más simple si el tiempo aprieta: búsqueda por coincidencia aproximada de texto (fuzzy search) sobre nombre, descripción y categoría, sin IA generativa — documentando en la tesis que quedó como mejora futura escalar a búsqueda semántica con embeddings.

### 9.3 Requisitos funcionales adicionales

- RF10: El sistema debe sugerir productos relacionados durante el registro de una venta, visible para Trabajador y Administrador.
- RF11: El sistema debe permitir buscar productos usando lenguaje natural/coloquial, no solo el nombre exacto del catálogo.

### 9.4 Dónde entra en los sprints

Estas funciones se agregan al **Sprint 4** (junto con reportes y cierre), ya que dependen de que el catálogo, las ventas y las presentaciones ya estén funcionando:

- Backend: endpoint de recomendaciones (nivel simple o avanzado, según se decida) y endpoint de búsqueda en lenguaje natural.
- Frontend web y móvil: mostrar las sugerencias de Iverita como una pequeña sección o modal durante la venta, y el cuadro de búsqueda inteligente con el nombre "Iverita" visible en la interfaz.

**Nota para los agentes:** no implementar nada del nivel avanzado (llamadas a LLM externo) sin que Kevin confirme qué proveedor de IA usar y tenga la API key lista — evita dejar credenciales hardcodeadas o llamadas sin control de costo/tiempo de respuesta.

---

## 10. Guía de diseño frontend (basada en el prototipo de Figma)

Para que el desarrollo en React sea consistente con el prototipo ya validado en Figma Make:

### 10.1 Paleta de colores (como variables/tokens, no hardcodeadas en cada componente)

```css
:root {
  --color-primary: #F5761A;       /* naranja principal — botones, acentos, gráficos */
  --color-primary-dark: #D9600A;  /* naranja hover/activo */
  --color-sidebar-bg: #1A1A1A;    /* negro/gris muy oscuro del sidebar */
  --color-bg: #FAF7F2;            /* blanco hueso — fondo general */
  --color-card-bg: #FFFFFF;       /* tarjetas */
  --color-success: #22C55E;       /* badges de estado "activo/completado" */
  --color-warning: #F5761A;       /* badges "stock bajo/en revisión" */
  --color-danger: #EF4444;        /* badges "crítico" */
  --color-text: #1A1A1A;
  --color-text-secondary: #6B7280;
}
```

### 10.2 Componentes y patrones a mantener

- **Tarjetas:** fondo blanco, esquinas redondeadas (`border-radius: 12px`), sombra suave (`box-shadow: 0 1px 3px rgba(0,0,0,0.08)`), sin bordes duros.
- **Botones primarios:** fondo `--color-primary`, texto blanco, esquinas redondeadas, sin bordes.
- **Badges de estado:** fondo de color suave (no sólido) con texto del color correspondiente — verde para "activo/completado", naranja para "stock bajo/en revisión", rojo para "crítico".
- **Sidebar:** fondo `--color-sidebar-bg`, ítem activo resaltado en naranja, íconos simples y consistentes (usar una sola librería de íconos, ej. lucide-react, en toda la app).
- **Gráficos:** usar el naranja principal como color base de barras/líneas; evitar mezclar más de 2-3 colores por gráfico.
- **Tipografía:** una sola familia sans-serif en toda la app (ej. Inter o similar), tamaños consistentes por jerarquía (título de tarjeta, valor destacado, texto secundario).

### 10.3 Recomendación técnica

Define estos colores como variables CSS (o tokens de Tailwind si usas Tailwind) desde el inicio del Sprint 1, no al final — así cada pantalla nueva que construyan los agentes hereda el mismo estilo automáticamente, sin que cada componente reinvente sus propios colores.