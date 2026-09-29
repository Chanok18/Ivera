# Reglas del proyecto Ivera

- Antes de cualquier tarea, lee `DOCUMENTACION.md` (fuente de verdad) y `PROGRESO.md` (estado actual).
- NUNCA modifiques `DOCUMENTACION.md`. Si algo choca con él, detente y pregúntame.
- Haz solo la tarea pedida. No adelantes funciones de otros sprints ni agregues tablas, roles, endpoints o dependencias que no estén en la documentación.
- Nunca escribas credenciales en el código: usa variables de entorno y `.env` (fuera de git).
- Para verificar que el código compila, usa siempre "mvn clean compile" (nunca solo "mvn compile"), porque Maven puede saltarse la recompilación y dar un falso "compila bien".
- Al terminar cada tarea: compila/prueba, y actualiza `PROGRESO.md` (qué hiciste, archivos clave, pendientes o dudas).