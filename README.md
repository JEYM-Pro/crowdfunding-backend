# Crowdfunding Platform Backend

Backend REST para la plataforma de Crowdfunding (CodeFactory UdeA), construido con Spring Boot, PostgreSQL (Supabase) y seguridad JWT.

---

## Estado Actual

Este repositorio ya incluye:

- Autenticación JWT stateless.
- Registro de usuarios (creadores y patrocinadores).
- Login con emisión de token.
- Gestión de perfil de usuario autenticado.
- Recarga de saldo (wallet).
- Consulta de balance con histórico y campañas asociadas.
- Manejo global de errores con formato estándar.
- Auditoría JPA (`created_at`, `updated_at`).
- Swagger/OpenAPI con Authorize para probar endpoints protegidos.

---

## Stack Técnico

- **Java** 21
- **Spring Boot** 4.1.1
- Spring Web MVC, Spring Data JPA, Spring Security, Spring Validation
- **JWT** (jjwt 0.12.5)
- **PostgreSQL** (Supabase)
- **Springdoc OpenAPI** (Swagger UI) 2.8.5
- **Maven Wrapper**

---

## Estructura Principal

```
src/main/java/co/udea/crowdfunding
├── config/          # Security, JWT, OpenAPI, CORS
├── controller/      # AuthController, UserController
├── dto/             # Request/Response records (contratos API)
├── entity/          # User, Campaign, Transaction (JPA)
├── exception/       # ApiError, EmailAlreadyExistsException, GlobalExceptionHandler
├── repository/      # Spring Data JPA repositories
├── service/         # AuthService, UserService (lógica de negocio)
└── CrowdfundingApplication.java

src/main/resources
└── application.properties
```

---

## Requisitos Previos

- **JDK 21**
- **Maven** (opcional, se recomienda usar `mvnw`)
- **PostgreSQL accesible** (actualmente configurado para Supabase)

---

## Variables y Configuración

La aplicación requiere al menos esta variable de entorno:

```bash
JWT_SECRET_KEY="clave-base64-para-firmar-jwt"
```

Ejemplo en PowerShell:
```powershell
$env:JWT_SECRET_KEY="TU_CLAVE_BASE64_AQUI"
```

En Linux/macOS:
```bash
export JWT_SECRET_KEY="TU_CLAVE_BASE64_AQUI"
```

**Notas:**

- La conexión a BD está definida en `application.properties` para entorno de desarrollo.
- Para despliegue real, se recomienda sobreescribir credenciales por variables de entorno:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

---

## Ejecutar Proyecto

### Windows (PowerShell)

```powershell
$env:JWT_SECRET_KEY="TU_CLAVE_BASE64_AQUI"
.\mvnw.cmd spring-boot:run
```

### Linux/macOS

```bash
export JWT_SECRET_KEY="TU_CLAVE_BASE64_AQUI"
./mvnw spring-boot:run
```

La API queda disponible en:

- **Base URL:** `http://localhost:8081`
- **Swagger UI:** `http://localhost:8081/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8081/v3/api-docs`

---

## Seguridad

### Endpoints públicos

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/auth/register` | Registrar nuevo usuario |
| POST | `/api/auth/login` | Iniciar sesión, retorna JWT Bearer |

### Endpoints protegidos (requieren JWT)

| Método | Ruta | Descripción |
|--------|------|-------------|
| PUT | `/api/users/profile` | Actualizar datos personales |
| POST | `/api/users/recharge` | Recargar saldo (wallet) |
| GET | `/api/users/balance` | Consultar balance, histórico y campañas |

### Roles

- `sponsor` (patrocinador) - rol por defecto
- `creator` (creador de campañas)

### Autenticación

El sistema usa JWT stateless. El token se obtiene en `POST /api/auth/login` y debe enviarse como **Bearer token** en el header `Authorization` de cada request protegido:

```http
Authorization: Bearer <token>
```

El filtro `JwtAuthenticationFilter` valida el token en cada petición y popula el `SecurityContext` con el `UserPrincipal` (id, email, role).

---

## Base de Datos

- **Motor:** PostgreSQL en Supabase (serverless)
- **ORM:** Spring Data JPA / Hibernate
- **Tablas principales:**
  - `users` — usuarios (creadores y patrocinadores)
  - `campaigns` — campañas de crowdfunding
  - `transactions` — movimientos (recargas, aportes, retiros)

La propiedad `spring.jpa.hibernate.ddl-auto=validate` evita cambios automáticos de esquema. Para producción se recomienda usar **Flyway** o **Liquibase** (pendiente).

---

## Endpoints Implementados

### 1) Registro de usuario

```http
POST /api/auth/register
Content-Type: application/json
```

**Request:**
```json
{
  "name": "Juan Pérez",
  "email": "juan@email.com",
  "password": "ClaveSegura123"
}
```

**Response (201 Created):**
```json
{
  "id": "uuid",
  "name": "Juan Pérez",
  "email": "juan@email.com",
  "message": "Cuenta creada correctamente"
}
```

**Validaciones:**
- Email único (409 Conflict si ya existe)
- Campos obligatorios (400 Bad Request)

---

### 2) Login

```http
POST /api/auth/login
Content-Type: application/json
```

**Request:**
```json
{
  "email": "juan@email.com",
  "password": "ClaveSegura123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "id": "uuid",
  "name": "Juan Pérez",
  "email": "juan@email.com",
  "role": "sponsor",
  "balance": 0
}
```

---

### 3) Actualizar perfil

```http
PUT /api/users/profile
Authorization: Bearer <token>
Content-Type: application/json
```

**Request:**
```json
{
  "name": "Juan Pérez Actualizado",
  "email": "juan.nuevo@email.com"
}
```

**Response (200 OK):**
```json
{
  "id": "uuid",
  "name": "Juan Pérez Actualizado",
  "email": "juan.nuevo@email.com",
  "role": "sponsor",
  "balance": 1000.00,
  "historic": 500.00
}
```

---

### 4) Recargar saldo (Wallet)

```http
POST /api/users/recharge
Authorization: Bearer <token>
Content-Type: application/json
```

**Request:**
```json
{
  "amount": 50000.00
}
```

**Response (200 OK):**
```json
{
  "id": "uuid",
  "balance": 51000.00,
  "message": "Recarga realizada exitosamente"
}
```

Crea una transacción de tipo `recharge` en el histórico.

---

### 5) Consultar balance y actividad

```http
GET /api/users/balance
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
{
  "balance": 51000.00,
  "historic": 500.00,
  "campaigns": [
    {
      "id": "uuid",
      "title": "Mi Campaña",
      "raised": 10000.00,
      "goal": 50000.00,
      "contributed": 0
    }
  ],
  "history": [
    {
      "id": "uuid",
      "amount": 50000.00,
      "type": "recharge",
      "date": "2026-09-22T13:00:00Z",
      "campaignName": null,
      "campaignId": null
    }
  ]
}
```

- Si el usuario es `creator`: muestra sus campañas con `raised` y `goal`.
- Si el usuario es `sponsor`: muestra campañas donde aportó con `contributed`.

---

## Formato de Errores

Las respuestas de error siguen un formato estándar:

```json
{
  "message": "Datos inválidos",
  "details": {
    "email": "El email es obligatorio",
    "password": "La contraseña debe tener al menos 6 caracteres"
  }
}
```

**Códigos HTTP comunes:**
- `400 Bad Request` — Validación fallida
- `401 Unauthorized` — Token inválido, expirado o faltante
- `409 Conflict` — Email ya registrado
- `500 Internal Server Error` — Error inesperado

---

## Swagger / OpenAPI

La documentación interactiva está disponible en:

- **Swagger UI:** `http://localhost:8081/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8081/v3/api-docs`

En Swagger UI:
1. Click en **Authorize** (🔒)
2. Pega el token JWT: `Bearer <tu_token>`
3. Prueba endpoints protegidos directamente desde la UI

El esquema de seguridad `bearerAuth` está configurado en `OpenApiConfig`.

---

## Comandos Útiles

```bash
# Compilar y validar
./mvnw -DskipTests validate

# Ejecutar pruebas
./mvnw test

# Empaquetar JAR
./mvnw package -DskipTests

# Limpiar y compilar
./mvnw clean compile
```

En Windows reemplazar `./mvnw` por `.\mvnw.cmd`.

---

## Próximos Pasos / Roadmap

- [ ] Gestión de campañas (CRUD para creadores)
- [ ] Aportes a campañas (patrocinadores)
- [ ] Retiro de fondos (creadores)
- [ ] Migraciones de BD con Flyway/Liquibase
- [ ] Tests de integración y unitarios
- [ ] CI/CD con GitHub Actions
- [ ] Métricas con Actuator + Prometheus/Grafana
- [ ] Rate limiting y hardening de seguridad

---

## Licencia

MIT — Uso académico CodeFactory UdeA.
---

## Campañas (HU-06 y HU-07)

**Antes de levantar el backend** ejecutar en Supabase (SQL Editor) el script
`src/main/resources/db/migration/V3__campaigns_publish_and_edit.sql`
(`ddl-auto=validate` no crea tablas ni columnas). Es idempotente.

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/campaigns` | HU-06: publica una campaña (queda `ACTIVE`). Devuelve 201. |
| `PUT` | `/api/campaigns/{id}` | HU-07: edita título, descripción, categoría e imágenes de una campaña activa. Meta y fecha solo si `raised = 0`. Las recompensas no se pueden modificar (409). |
| `GET` | `/api/campaigns` | Listado público de campañas activas. |
| `GET` | `/api/campaigns/{id}` | Detalle de una campaña. |
| `GET` | `/api/categories` | Categorías para el selector del formulario. |

Reglas: meta > 0; fecha límite posterior a hoy y máximo 90 días; hasta 4 imágenes (URL http/https);
categoría obligatoria (Tecnología, Agricultura, Arte y cultura, Educación, Medio ambiente, Comunidad, Salud);
hasta 6 recompensas opcionales, cada una con monto mínimo y unidades disponibles mayores a cero, que quedan fijas al publicar. Errores: 400 (validación), 403 (no es el creador), 404 (no existe), 409 (campaña no activa o con aportes).

Pruebas y cobertura: `mvn test` genera el reporte JaCoCo en `target/site/jacoco/index.html`.

---

## Aportes con recompensa y comprobante (HU-10, HU-11 y HU-12)

**Antes de levantar el backend**

1. Ejecutar en Supabase (SQL Editor) `src/main/resources/db/migration/V4__contributions_rewards_and_receipts.sql`
   (permite `stock = 0` en `rewards` y agrega `reward_id` / `reward_name` a `transactions`). Es idempotente.
2. Configurar el correo por variables de entorno (no se guardan credenciales en el código):

| Variable | Uso | Ejemplo |
|---|---|---|
| `SMTP_HOST` | Servidor SMTP | `smtp.gmail.com` |
| `SMTP_PORT` | Puerto | `587` |
| `SMTP_USERNAME` | Cuenta SMTP | cuenta remitente |
| `SMTP_PASSWORD` | Contraseña o clave de aplicación | no subir al repositorio |
| `SMTP_AUTH` | Activar autenticación | `true` |
| `SMTP_STARTTLS` | Activar STARTTLS | `true` |
| `SMTP_FROM` | Dirección remitente autorizada | la del proveedor |

Si `SMTP_HOST` o `SMTP_FROM` no están configurados, o el proveedor rechaza el envío, **el aporte no se confirma** (error 500 y todo se revierte).

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/campaigns/{id}/contributions/confirm` | HU-11/HU-12: aporta con `rewardId` opcional y devuelve el comprobante (201). |
| `POST` | `/api/campaigns/{id}/contributions` | HU-10: misma ruta y respuesta de texto; acepta `rewardId` opcional y comparte el mismo procesamiento (incluido el correo). |

Body: `{ "amount": 60000, "rewardId": "uuid-opcional" }`. El `rewardId` debe ser uno de los `rewards` de `GET /api/campaigns/{id}`.

Validaciones (en este orden): monto > 0; campaña existente y activa; recompensa de esa campaña; stock > 0
(con stock `0` el detalle de campaña lo devuelve como agotado); monto >= `minAmount` de la recompensa; saldo suficiente.
Errores: 400 (regla de negocio o validación), 404 (campaña inexistente), 409 (campaña no activa), 500 (correo no disponible).
Al confirmar se descuenta el saldo, se suma al total histórico aportado (`historic`) y al recaudo de la campaña, se reduce el stock y se guarda la transacción
(`type = contribution`, con `rewardId` y `rewardName`). La campaña y el patrocinador se bloquean (`PESSIMISTIC_WRITE`) para evitar aportes simultáneos sobre la última unidad o el mismo saldo.
