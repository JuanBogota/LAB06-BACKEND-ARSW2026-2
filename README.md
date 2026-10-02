# Lab 6 – Backend de BluePrints en Tiempo Real (Java 21 / Spring Boot 3.3.x)

**Escuela Colombiana de Ingeniería – Arquitecturas de Software**

**Autores:** Carlos Duban Rojas y Juan Daniel Bogotá Fuentes

Este repositorio es el **backend** del Laboratorio 6 (BluePrints en tiempo real). Es la API REST de planos y puntos que construimos en el laboratorio 3, a la que se le agregó **tiempo real con STOMP** para que varias personas dibujen el mismo plano a la vez.

| Repositorio | Contenido |
|---|---|
| **Backend** (este) | API REST + WebSocket/STOMP |
| [Frontend](https://github.com/JuanBogota/LAB06-FRONTEND-ARSW2026-2) | Interfaz en React + Vite, y **el informe del laboratorio** (decisiones, evidencia y análisis) |

> El informe completo del laboratorio 6 está en el README del repositorio del frontend. Este README explica solo cómo ejecutar y usar el backend.
> El informe original del laboratorio 3 (REST, PostgreSQL, Swagger y filtros) se conserva en [`LAB03-INFORME.md`](LAB03-INFORME.md).

---

## 📋 Requisitos

- Java 21 (el proyecto compila con `release 21`; un JDK más nuevo también sirve)
- Maven 3.9+
- Docker (para PostgreSQL)

## ▶️ Cómo ejecutarlo

```bash
# 1) Levantar PostgreSQL (usa compose.yaml)
docker compose up -d

# 2) Arrancar la API en http://localhost:8080
mvn spring-boot:run
```

Los datos de conexión a la base están en `src/main/resources/application.properties` y son solo para desarrollo. En un sistema real irían en variables de entorno.

Si el arranque falla con `Port 8080 was already in use`, hay otro programa usando ese puerto. Ciérralo y vuelve a intentar.

**Comprobar que responde:**

```powershell
curl.exe -i http://localhost:8080/api/v1/blueprints
```

Debe devolver `200` con la lista de planos dentro de `data`.

**Documentación interactiva:**
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

---

## 🌐 API REST

Todas las rutas empiezan por `/api/v1/blueprints` y todas las respuestas vienen en este formato:

```json
{ "code": 200, "message": "execute ok", "data": { } }
```

| Método | Ruta | Qué hace | Respuesta |
|---|---|---|---|
| GET | `/api/v1/blueprints` | Lista todos los planos | 200 |
| GET | `/api/v1/blueprints/{author}` | Lista los planos de un autor | 200 / 404 |
| GET | `/api/v1/blueprints/{author}/{bpname}` | Devuelve un plano con sus puntos | 200 / 404 |
| POST | `/api/v1/blueprints` | Crea un plano | 201 / 400 |
| PUT | `/api/v1/blueprints/{author}/{bpname}/points` | Agrega **un** punto a un plano | 202 / 404 |

> **En desarrollo** (lo está haciendo el equipo para el CRUD de la interfaz): `PUT /api/v1/blueprints/{author}/{name}` que reemplaza todos los puntos, `DELETE /api/v1/blueprints/{author}/{name}`, y que `GET /api/v1/blueprints/{author}` devuelva una lista vacía cuando el autor no tiene planos. Esta tabla se actualiza cuando se integren.

**Ejemplo: crear un plano** (en PowerShell, con el JSON en un archivo para que las comillas no se rompan):

```powershell
'{"author":"juan","name":"plano-1","points":[{"x":10,"y":10},{"x":40,"y":50},{"x":120,"y":60}]}' | Out-File -Encoding ascii plano.json
curl.exe -i -X POST http://localhost:8080/api/v1/blueprints -H "Content-Type: application/json" --data-binary "@plano.json"
```

Respuesta esperada (`201`):

```json
{"code":201,"message":"resource created","data":{"author":"juan","name":"plano-1","points":[{"x":10,"y":10},{"x":40,"y":50},{"x":120,"y":60}]}}
```

Si el plano ya existe, responde `400`.

---

## ⚡ Tiempo real (STOMP sobre WebSocket)

Cuando alguien hace clic en el canvas, el navegador envía el punto al servidor; el servidor lo valida y lo reenvía a todos los que están viendo **el mismo plano**.

| Qué | Valor |
|---|---|
| Conexión WebSocket | `ws://localhost:8080/ws-blueprints` |
| Enviar un punto a | `/app/draw` |
| Escuchar un plano | `/topic/blueprints.{author}.{name}` |

**Lo que se envía** a `/app/draw`:

```json
{ "author": "juan", "name": "plano-1", "point": { "x": 262, "y": 203 } }
```

**Lo que llega** a quienes escuchan `/topic/blueprints.juan.plano-1`:

```json
{ "author": "juan", "name": "plano-1", "points": [ { "x": 262, "y": 203 } ] }
```

Reglas importantes:

- **Llega solo el punto nuevo**, no el plano completo. El estado inicial se pide por REST.
- **El tiempo real no guarda nada.** Los puntos dibujados en vivo se guardan cuando el usuario presiona Save (que usa la API REST).
- **Se validan los nombres:** `author` y `name` solo pueden tener letras, números, `_` y `-`. Si no cumplen, el mensaje se rechaza (se registra un `WARN` en el log) y no se envía a nadie. Esto evita que nombres como `a.b` se confundan con otros canales.

---

## 🗂️ Arquitectura

```
src/main/java/edu/eci/arsw/blueprints
  ├── model/         # Dominio: Blueprint, Point, ApiResponse
  ├── persistence/   # Interfaz BlueprintPersistence + implementaciones (Postgres, InMemory)
  ├── services/      # Casos de uso: BlueprintsServices, DrawingService
  │                  # y el puerto de salida BlueprintEventPublisher
  ├── filters/       # Filtros de puntos (Identity, Redundancy, Undersampling)
  ├── controllers/   # REST: BlueprintsAPIController, GlobalExceptionHandler
  ├── realtime/      # Adaptadores de tiempo real: DrawController, StompBlueprintEventPublisher
  └── config/        # WebSocketConfig, CorsConfig, OpenApiConfig
```

El tiempo real sigue el patrón de **puertos y adaptadores** (Arquitectura Hexagonal), para que la lógica no dependa de la tecnología de mensajería:

```
 Adaptador de entrada          Núcleo (no conoce STOMP)         Adaptador de salida
 DrawController           →    DrawingService  →  [puerto]  →   StompBlueprintEventPublisher
 @MessageMapping("/draw")      (caso de uso)      BlueprintEventPublisher
```

Si algún día se cambia STOMP por otra tecnología, solo hay que reemplazar un adaptador.

## ⚙️ Configuración

| Propiedad | Para qué sirve | Valor por defecto |
|---|---|---|
| `app.cors.allowed-origins` | Orígenes que pueden llamar a la API y al WebSocket (separados por coma si son varios) | `http://localhost:5173` |

**Filtros de puntos:** por defecto no se modifica nada (`IdentityFilter`). Se pueden activar con los perfiles de Spring `redundancy` (quita puntos duplicados seguidos) y `undersampling` (deja 1 de cada 2 puntos).

## ⚠️ Limitaciones conocidas

- El *broker* de mensajes es el que trae Spring en memoria, así que solo sirve con **una instancia** de la API. Para escalar a varias instancias habría que usar un broker externo (por ejemplo RabbitMQ).
- Los puntos dibujados en vivo no se guardan hasta presionar Save: quien entra tarde o recarga solo ve lo guardado.
- El servidor no envía latidos (*heartbeats*) a los clientes WebSocket.
