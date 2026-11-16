# Meeting Rooms API

API REST para reservar salas de reuniones. Java 17 + Spring Boot 3.5, Flyway, H2/PostgreSQL y HTTP Basic para las operaciones de escritura.

## Inicio rápido

Requisitos: **JDK 17+** (no hace falta instalar Maven, se usa el wrapper).

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Credenciales de escritura (solo desarrollo): `admin` / `admin123`, se cambian con `APP_USERNAME` y `APP_PASSWORD`.

Ejecutar los tests:

```bash
./mvnw test
```

### Con PostgreSQL

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

La conexión se puede sobrescribir con `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

## Endpoints

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `POST` | `/api/v1/rooms` | Sí | Crea una sala |
| `GET` | `/api/v1/rooms` | No | Lista las salas (ordenadas por nombre) |
| `GET` | `/api/v1/rooms/{id}` | No | Detalle de una sala |
| `POST` | `/api/v1/reservations` | Sí | Crea una reserva |
| `GET` | `/api/v1/reservations` | No | Lista paginada con filtros `roomId`, `date`, `status` |
| `GET` | `/api/v1/reservations/{id}` | No | Detalle de una reserva |
| `POST` | `/api/v1/reservations/{id}/cancel` | Sí | Cancela una reserva |

### Ejemplo

```bash
curl -u admin:admin123 -H "Content-Type: application/json" \
  -d '{"name":"Orion","capacity":8}' http://localhost:8080/api/v1/rooms

curl -u admin:admin123 -H "Content-Type: application/json" \
  -d '{"roomId":1,"responsible":"Ada Lovelace","start":"2030-01-15T10:00:00","end":"2030-01-15T11:00:00"}' \
  http://localhost:8080/api/v1/reservations

curl "http://localhost:8080/api/v1/reservations?roomId=1&date=2030-01-15&page=0&size=20&sort=startTime,asc"
```

### Códigos de respuesta

| Código | Cuándo |
|--------|--------|
| `201` | Sala o reserva creada (con cabecera `Location`) |
| `400` | Datos inválidos: campos obligatorios, `end <= start`, `start` en el pasado, fecha mal formada, filtro u orden inválido |
| `401` | Escritura sin credenciales o con credenciales incorrectas |
| `404` | Sala o reserva inexistente |
| `409` | La sala ya está ocupada en ese horario, nombre de sala duplicado, o reserva ya cancelada/finalizada |

Todos los errores usan el formato estándar [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457). Los de validación incluyen un array `errors` por campo:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/api/v1/reservations",
  "errors": [
    { "field": "end", "message": "end is required" },
    { "field": "responsible", "message": "responsible is required" }
  ]
}
```

## Decisiones

| Tema | Decisión | Por qué |
|------|----------|---------|
| Estructura | Paquetes por funcionalidad (`room`, `reservation`, `shared`) | La estructura cuenta de qué trata el sistema. Para este tamaño, una arquitectura hexagonal completa (puertos/adaptadores) añadiría ceremonia sin beneficio. |
| Intervalos | Semiabiertos `[start, end)` modelados en el value object `TimeSlot` | Una reunión que termina a las 10:00 no choca con otra que empieza a las 10:00. La regla vive en un solo lugar y se prueba sin Spring. |
| Concurrencia (reservas) | `SELECT ... FOR UPDATE` sobre la fila de la sala antes de comprobar solapamientos | Sin bloqueo, dos peticiones simultáneas podrían pasar la comprobación y crear una doble reserva. Bloquear la sala serializa solo las reservas de esa sala. Hay un test con 8 hilos que lo demuestra: sin el lock se crean las 8. |
| Concurrencia (cancelación) | Bloqueo optimista con `@Version` | Dos cancelaciones simultáneas no pueden devolver ambas `200`; una gana y la otra recibe `409`. |
| Cancelación | Cambio de estado (`CANCELLED` + `cancelledAt`) vía `POST /{id}/cancel`, no `DELETE` | Se conserva el histórico y el hueco queda libre. Cancelar dos veces o una reserva ya terminada devuelve `409`. |
| Fechas | `LocalDateTime` en hora local de la oficina, ISO-8601; zona configurable con `APP_TIME_ZONE` | Una sala es física y vive en una zona horaria. "Ahora" se evalúa en la zona de la oficina y no en la del servidor o contenedor. |
| Filtro por fecha | Devuelve las reservas que se **solapan** con ese día | Una reunión de 23:00 a 01:00 aparece en ambos días, que es lo que espera quien consulta la agenda. |
| Paginación | `page`, `size` (máx. 100) y `sort`; respuesta `PageResponse` propia | Contrato JSON estable, sin depender de cómo Spring serializa `PageImpl`. Ordenar por una propiedad desconocida da `400`, no `500`. |
| Base de datos | H2 en memoria (modo PostgreSQL) por defecto, perfil `postgres` con `compose.yaml` | Se ejecuta sin instalar nada; las migraciones Flyway son SQL estándar válido en ambos motores. Las restricciones (`CHECK`, `UNIQUE`, `FK`) protegen también a nivel de BD. |
| Seguridad | HTTP Basic sin estado, usuario en configuración con BCrypt | Es lo mínimo suficiente para el requisito. Sin sesión ni cookies, CSRF no aplica. El `401` devuelve el mismo formato que el resto de errores. |
| Tiempo | `Clock` inyectado como bean | Las reglas que dependen de "ahora" (no reservar en el pasado, no cancelar algo terminado) se prueban de forma determinista. |
| `open-in-view` | Desactivado | Evita consultas perezosas ocultas fuera de la transacción; las relaciones necesarias se cargan con `@EntityGraph`. |

## Tests

| Tipo | Clase | Qué cubre |
|------|-------|-----------|
| Unitario | `TimeSlotTest` | Regla de solapamiento (bordes incluidos) y `end > start` |
| Unitario | `ReservationServiceTest` | Reserva libre, conflicto `409`, pasado, sala inexistente, cancelación (Mockito + `Clock` fijo) |
| Integración (MockMvc) | `ReservationControllerIntegrationTest` | Flujo HTTP completo con H2 + Flyway: creación, `409`, `400`, cancelación, paginación y filtros |
| Integración (MockMvc) | `RoomControllerIntegrationTest` | Alta, listado, validación, duplicados y `404` de salas |
| Integración (MockMvc) | `SecurityIntegrationTest` | Lectura pública, escritura con `401` / credenciales válidas |
| Integración (concurrencia) | `ConcurrencyIntegrationTest` | 8 reservas simultáneas del mismo hueco → solo 1 éxito; 8 cancelaciones simultáneas → solo 1 éxito |
| Unitario | `ClockConfigTest`, `GlobalExceptionHandlerTest` | Zona horaria configurable; los `500` no filtran detalles internos |

## Con más tiempo

- **Restricción de exclusión en PostgreSQL** (`EXCLUDE USING gist (room_id WITH =, tsrange(start_time, end_time) WITH &&) WHERE status = 'ACTIVE'`): la BD garantizaría la no superposición incluso fuera de la aplicación. No la añadí porque H2 no la soporta y no podía validarla aquí.
- **Unicidad de nombre de sala sin distinguir mayúsculas también en BD** (índice sobre `lower(name)`): hoy la aplicación lo comprueba, pero dos altas simultáneas de "Orion" y "orion" podrían colarse.
- **Testcontainers con PostgreSQL** para que los tests de integración corran contra el mismo motor que producción (en mi entorno no había Docker).
- **JWT y usuarios persistidos con roles**, y guardar quién creó/canceló cada reserva; solo el responsable o un admin podría cancelar.
- **Reglas de negocio configurables**: duración máxima, horario laboral, antelación máxima, capacidad vs. asistentes.
- **Zona horaria explícita** por sala (o API con `OffsetDateTime`) si hubiera oficinas en varios países.
- **Idempotencia** (`Idempotency-Key`) en la creación de reservas para reintentos seguros del cliente.
- Observabilidad (métricas de conflictos, logs estructurados), Dockerfile y pipeline de CI.

## Uso de IA

Usé Claude Code (agente de IA) para desarrollar casi toda la implementación. Mi rol fue dirigir el trabajo y verificar el resultado.

**Qué le pedí**
- Construir la API según el enunciado, con buenas prácticas y commits incrementales por unidad de trabajo.
- Explicarme cada decisión técnica y el problema que resuelve.

**Qué acepté**
- La estructura por funcionalidad, el bloqueo pesimista sobre la sala, los intervalos semiabiertos `[start, end)` y la cancelación como cambio de estado. Antes de aceptarlas pedí que me explicara qué problema resolvía cada una.

**Qué corregí o exigí**
- Commits y pushes por etapas en lugar de una única entrega al final.
- Usar mi identidad personal de git en todos los commits.
- Ver la aplicación funcionando en vivo (Swagger y una demo de respuestas `201`, `400`, `401` y `409`) además de los tests.

**Cómo se verificó la calidad**
- Durante el desarrollo el propio agente detectó y corrigió errores: tests de integración que Maven no ejecutaba (nombres `*IT`), finales de línea CRLF que rompían `mvnw` en Linux y un `500` al ordenar por un campo inexistente.
- Pedí una revisión independiente con un agente de contexto limpio. De ella salieron el test de concurrencia real (validado quitando el bloqueo a propósito: sin él se creaban 8 reservas en lugar de 1), `@Version` para cancelaciones simultáneas, el `500` en formato Problem Details y la zona horaria configurable.

Mi siguiente paso es estudiar el código a fondo para poder defender y modificar cada decisión por mi cuenta.