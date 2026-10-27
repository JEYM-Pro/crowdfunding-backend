# Crowdfunding Platform Backend

API REST de la plataforma de Crowdfunding (CodeFactory UdeA), construida con Spring Boot 4, PostgreSQL (Supabase) y seguridad JWT. Expone autenticación, billetera, campañas, aportes con recompensa y comprobante por correo, y administración de usuarios.

- **API en producción:** https://crowdfunding-backend-udea.duckdns.org
- **Swagger UI (producción):** https://crowdfunding-backend-udea.duckdns.org/swagger-ui/index.html
- **Frontend:** [juanbah0s/Crowdfunding](https://github.com/juanbah0s/Crowdfunding) · app en https://main.d1w138aotlfajn.amplifyapp.com

---

## Equipo

- Marino de Jesús Gómez Salazar
- Juan Esteban Yepes Martinez
- Juan Joel Cortes Montes
- Cristian Mateo Sepulveda Ochoa
- Yeison Alexis Pulgarin Velez

---

## Contenido

- [Estado actual](#estado-actual)
- [Stack técnico](#stack-técnico)
- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Requisitos previos](#requisitos-previos)
- [Configuración](#configuración)
- [Base de datos y migraciones](#base-de-datos-y-migraciones)
- [Ejecutar en local](#ejecutar-en-local)
- [Seguridad y roles](#seguridad-y-roles)
- [Endpoints](#endpoints)
- [Reglas de negocio](#reglas-de-negocio)
- [Formato de errores](#formato-de-errores)
- [Swagger / OpenAPI](#swagger--openapi)
- [Pruebas y cobertura](#pruebas-y-cobertura)
- [Despliegue en producción](#despliegue-en-producción)
- [Limitaciones conocidas](#limitaciones-conocidas)
- [Roadmap](#roadmap)

---

## Estado actual

| Módulo | Historias de usuario | Estado |
|---|---|---|
| Registro, login y perfil | HU-01, HU-02, HU-03 | ✅ |
| Billetera: recarga, saldo e historial | HU-04, HU-05 | ✅ |
| Publicar y editar campañas | HU-06, HU-07 | ✅ |
| Pausar, cerrar y reactivar campañas | HU-08 | ✅ |
| Explorar campañas: búsqueda, categoría y orden | HU-09 | ✅ |
| Aportes, recompensas con stock y comprobante por correo | HU-10, HU-11, HU-12 | ✅ |
| Administración de usuarios | HU-21 | ✅ |
| Categorías | — | ✅ |

---

## Stack técnico

- **Java** 21
- **Spring Boot** 4.1.1: Web MVC, Data JPA, Security, Validation, Mail
- **JWT** (jjwt 0.12.5)
- **PostgreSQL** en Supabase (conexión por *pooler*)
- **Springdoc OpenAPI** 2.8.5 (Swagger UI)
- **JUnit 5 + Mockito** para pruebas y **JaCoCo** para cobertura
- **Maven Wrapper** (`mvnw`)

---

## Arquitectura

El backend sigue una **arquitectura por capas**. Cada capa solo llama a la de abajo:

```
Controladores REST  (controller)   → reciben HTTP, validan DTOs, devuelven JSON
        ↓
Servicios           (service)      → reglas de negocio y casos de uso
        ↓
Repositorios JPA    (repository)   → acceso a datos (interfaces Spring Data)
        ↓
PostgreSQL (Supabase)
```

La seguridad es transversal: `JwtAuthenticationFilter` valida el token antes de que la petición llegue a los controladores.

Los diagramas de clases y C4 (contexto, contenedores y componentes) se entregan aparte, en la documentación del proyecto.

---

## Estructura del proyecto

```
src/main/java/co/udea/crowdfunding
├── config/       # SecurityConfig, JwtUtil, JwtAuthenticationFilter, UserPrincipal, OpenApiConfig, ClockConfig
├── controller/   # AuthController, UserController, CampaignController, CategoryController, AdminController
├── dto/          # Records de petición y respuesta (contratos de la API)
├── entity/       # User, Campaign, CampaignStatus, CampaignStatusHistory, Category, Reward, Transaction
├── exception/    # Excepciones del dominio, ApiError y GlobalExceptionHandler
├── repository/   # Repositorios Spring Data JPA (uno por entidad)
├── service/      # AuthService, UserService, CampaignService, AdminUserService, ContributionEmailService
└── CrowdfundingApplication.java

src/main/resources
├── application.properties
└── db/migration/ # Scripts SQL V2 a V6 (se ejecutan a mano en Supabase)

src/test/java/co/udea/crowdfunding   # Pruebas de entidades, servicios, DTOs, controladores y errores
```

---

## Requisitos previos

- **JDK 21**
- **Maven** (opcional, el proyecto incluye `mvnw`)
- Acceso a una base de datos **PostgreSQL** (el proyecto usa Supabase)
- Una cuenta **SMTP** para enviar los comprobantes de aporte (por ejemplo, Gmail con contraseña de aplicación)

---

## Configuración

La configuración está en `src/main/resources/application.properties`.

| Propiedad / variable | Uso | Valor por defecto |
|---|---|---|
| `SERVER_PORT` | Puerto HTTP | `8081` (en producción se usa `8080`) |
| `spring.datasource.*` | Conexión a PostgreSQL (Supabase) | Definida en el archivo |
| `jwt.secret`, `jwt.expiration` | Firma y duración del token (24 h) | Definidos en el archivo |
| `SMTP_HOST` | Servidor SMTP | `smtp.gmail.com` |
| `SMTP_PORT` | Puerto SMTP | `587` |
| `SMTP_USERNAME` | Cuenta SMTP | — |
| `SMTP_PASSWORD` | Contraseña o clave de aplicación | — (nunca subirla al repositorio) |
| `SMTP_AUTH` / `SMTP_STARTTLS` | Autenticación y TLS | `true` / `true` |
| `SMTP_FROM` | Dirección remitente | — |

Spring Boot permite sobreescribir cualquier propiedad con una variable de entorno, por ejemplo `SPRING_DATASOURCE_PASSWORD` o `JWT_SECRET`.

> ⚠️ **Sin correo configurado no hay aportes.** Si `SMTP_HOST` o `SMTP_FROM` faltan, o el proveedor rechaza el envío, el aporte se revierte y la API responde 500.

> ⚠️ **Pendiente de seguridad:** la contraseña de la base de datos y el secreto JWT todavía están escritos en `application.properties`. Ver [Limitaciones conocidas](#limitaciones-conocidas).

---

## Base de datos y migraciones

- **Motor:** PostgreSQL en Supabase. **ORM:** Spring Data JPA / Hibernate.
- `spring.jpa.hibernate.ddl-auto=validate`: Hibernate **no crea ni modifica tablas**, solo verifica que el esquema coincida. Por eso hay que ejecutar los scripts a mano.

**Tablas:** `users`, `campaigns`, `campaign_images`, `rewards`, `categories`, `transactions`, `campaign_status_history`.

**Scripts** (en `src/main/resources/db/migration/`; todos son idempotentes). Ejecutarlos en orden en el SQL Editor de Supabase:

| Script | Qué hace |
|---|---|
| `V2__add_user_status.sql` | Agrega el estado del usuario (`ACTIVE` / `INACTIVE`) |
| `V3__campaigns_publish_and_edit.sql` | Tablas y columnas de campañas, imágenes, recompensas y categorías |
| `V4__contributions_rewards_and_receipts.sql` | Permite `stock = 0` y agrega `reward_id` / `reward_name` a `transactions` |
| `V5__campaign_status_constraint.sql` | Restricción de estados de campaña (`ACTIVE`, `PAUSED`, `CLOSED`, `FINISHED`) |
| `V6__campaign_image_url_to_text.sql` | Permite guardar imágenes como *data URL* de hasta 2 MB |

---

## Ejecutar en local

**Linux / macOS**

```bash
export SMTP_USERNAME="tu_correo@gmail.com"
export SMTP_PASSWORD="tu_clave_de_aplicacion"
export SMTP_FROM="tu_correo@gmail.com"
./mvnw spring-boot:run
```

**Windows (PowerShell)**

```powershell
$env:SMTP_USERNAME="tu_correo@gmail.com"
$env:SMTP_PASSWORD="tu_clave_de_aplicacion"
$env:SMTP_FROM="tu_correo@gmail.com"
.\mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8081` y Swagger en `http://localhost:8081/swagger-ui.html`.

**Comandos útiles**

```bash
./mvnw clean compile          # Compilar
./mvnw test                   # Pruebas + reporte de cobertura
./mvnw package -DskipTests    # Generar target/*.jar
```

En Windows reemplazar `./mvnw` por `.\mvnw.cmd`.

---

## Seguridad y roles

- Autenticación **JWT stateless**. El token se obtiene en `POST /api/auth/login` y se envía en cada petición protegida:

  ```http
  Authorization: Bearer <token>
  ```

- `JwtAuthenticationFilter` valida el token y carga el `UserPrincipal` (id, email, rol) en el contexto de seguridad.
- Contraseñas guardadas con **BCrypt** (`PasswordEncoder`).

| Rol | Descripción |
|---|---|
| `sponsor` | Patrocinador. Rol por defecto al registrarse |
| `creator` | Creador. Un usuario pasa a `creator` automáticamente al publicar su primera campaña |
| `admin` | Administrador. Acceso a `/api/admin/**` |

**Acceso a rutas** (`SecurityConfig`):

| Rutas | Acceso |
|---|---|
| `/api/auth/**`, `GET /api/categories`, `GET /api/campaigns`, `GET /api/campaigns/{id}`, Swagger | Público |
| `/api/campaigns/mine`, `/api/users/**`, publicar, editar y aportar | Requiere token |
| `/api/admin/**` | Solo `admin` |

---

## Endpoints

Todos los ejemplos y esquemas completos están en Swagger. Resumen:

### Autenticación

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/register` | Registrar usuario (409 si el correo ya existe) |
| POST | `/api/auth/login` | Iniciar sesión; devuelve el token JWT, el rol y el saldo |

### Usuarios (billetera y perfil)

| Método | Ruta | Descripción |
|---|---|---|
| PUT | `/api/users/profile` | Editar nombre y correo |
| POST | `/api/users/recharge` | Recargar saldo (crea una transacción `recharge`) |
| GET | `/api/users/balance` | Saldo, total aportado, historial de transacciones y campañas propias o apoyadas |

### Campañas y aportes

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/campaigns?nombre=&categoria=&orden=` | Campañas activas. `orden`: `recaudo`, `fecha`, `popularidad` (por defecto, las más recientes) |
| GET | `/api/campaigns/{id}` | Detalle con recompensas, stock, patrocinadores y número de aportes |
| GET | `/api/campaigns/mine` | Campañas del creador (incluye pausadas y cerradas); un admin ve todas |
| POST | `/api/campaigns` | **HU-06:** publicar campaña (queda `ACTIVE`). 201 |
| PUT | `/api/campaigns/{id}` | **HU-07:** editar campaña activa (solo su creador) |
| PATCH | `/api/campaigns/{id}/status` | **HU-08:** cambiar estado (`ACTIVE`, `PAUSED`, `CLOSED`); solo el creador |
| POST | `/api/campaigns/{id}/contributions/confirm` | **HU-11/HU-12:** aportar con recompensa opcional; devuelve el comprobante (201) y lo envía por correo |
| POST | `/api/campaigns/{id}/contributions` | **HU-10:** mismo proceso, con respuesta en texto |

Body de un aporte: `{ "amount": 60000, "rewardId": "uuid-opcional" }`.

### Categorías

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/categories` | Listar categorías (Tecnología, Agricultura, Arte y cultura, Educación, Medio ambiente, Comunidad, Salud) |
| POST | `/api/categories` | Crear categoría |

### Administración de usuarios (solo `admin`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/admin/users?page=0&size=20&sort=createdAt,desc&email=&role=&status=` | Listado paginado con filtros |
| GET | `/api/admin/users/{id}` | Detalle de un usuario |
| POST | `/api/admin/users` | Crear usuario (`sponsor` o `creator`). 201 |
| PUT | `/api/admin/users/{id}` | Editar nombre, correo y rol |
| PATCH | `/api/admin/users/{id}/status` | Activar o desactivar: `{ "action": "DISABLE" \| "ENABLE" }`. 204 |

---

## Reglas de negocio

**Campañas (HU-06 a HU-08)**
- Meta mayor a 0; fecha límite posterior a hoy y como máximo a 90 días.
- Hasta 4 imágenes y hasta 6 recompensas opcionales (monto mínimo y unidades mayores a 0). Las recompensas quedan fijas al publicar.
- Solo el creador puede editar o cambiar el estado de su campaña (403 si no).
- Si la campaña ya recibió aportes, **la meta y la fecha límite quedan bloqueadas** (409).
- Una campaña pausada o cerrada no recibe aportes.

**Aportes (HU-10 a HU-12)** — validaciones en este orden:
1. Monto mayor a 0.
2. Campaña existente y activa.
3. La recompensa pertenece a la campaña, tiene stock y el monto alcanza su mínimo.
4. Saldo suficiente.

Al confirmar, en una sola transacción: se descuenta el saldo, se suma al total aportado (`historic`) y al recaudo de la campaña, se baja una unidad de stock, se guarda la transacción (`type = contribution`) y se envía el comprobante por correo. Si algo falla, incluido el correo, **todo se revierte**. La campaña y el patrocinador se bloquean (`PESSIMISTIC_WRITE`) para evitar dos aportes simultáneos sobre la última unidad o el mismo saldo.

---

## Formato de errores

Todas las respuestas de error usan el mismo formato (`ApiError`):

```json
{
  "message": "Datos inválidos",
  "details": {
    "email": "El email es obligatorio"
  }
}
```

| Código | Cuándo |
|---|---|
| 400 | Validación fallida o regla de negocio (saldo insuficiente, recompensa agotada…) |
| 401 | Token inválido, expirado o faltante |
| 403 | Sin permisos (no es el creador, no es admin) |
| 404 | Recurso inexistente |
| 409 | Conflicto: correo repetido, campaña no activa o con aportes |
| 500 | Error inesperado o correo no disponible al aportar |

---

## Swagger / OpenAPI

- **Producción:** https://crowdfunding-backend-udea.duckdns.org/swagger-ui/index.html
- **Local:** `http://localhost:8081/swagger-ui.html`
- **OpenAPI JSON:** `/v3/api-docs`

Para probar endpoints protegidos:
1. Ejecutar `POST /api/auth/login` y copiar el `token`.
2. Pulsar **Authorize** y pegar el token.
3. Probar los endpoints desde la interfaz.

---

## Pruebas y cobertura

```bash
./mvnw test
```

- **97 pruebas** (JUnit 5 + Mockito) en 8 clases: `CampaignTest`, `CampaignServiceTest`, `CampaignContributionConfirmationTest`, `ContributionEmailServiceTest`, `CampaignRequestValidationTest`, `CampaignControllerTest`, `GlobalExceptionHandlerCampaignTest` y `GlobalExceptionHandlerContributionTest`.
- Reporte de cobertura de JaCoCo: `target/site/jacoco/index.html` (*quality gate* ≥ 65 %).

---

## Despliegue en producción

| Pieza | Dónde |
|---|---|
| API | AWS EC2 (Ubuntu) con dominio `crowdfunding-backend-udea.duckdns.org` |
| Proxy HTTPS | Nginx → `127.0.0.1:8080` |
| Proceso | Servicio systemd `crowdfunding` (se reinicia solo si se cae) |
| Base de datos | Supabase (PostgreSQL) |
| Frontend | AWS Amplify |

**Archivos en el servidor**
- JAR: `/home/ubuntu/app.jar` · Logs: `/home/ubuntu/app.log`
- Variables SMTP: `/etc/crowdfunding.env` (permisos `600`), cargadas por el servicio con `EnvironmentFile`.

**Publicar una versión nueva**

```bash
./mvnw package -DskipTests
scp -i <llave.pem> target/*.jar ubuntu@<ip-del-servidor>:/home/ubuntu/app.jar
ssh -i <llave.pem> ubuntu@<ip-del-servidor> 'sudo systemctl restart crowdfunding'
```

**Revisar el servicio**

```bash
sudo systemctl status crowdfunding     # Estado
tail -f /home/ubuntu/app.log           # Log en vivo
```

> Si el frontend muestra errores de CORS, primero comprobar que la API responde: un 502 de Nginx significa que el servicio está caído, no que CORS esté mal configurado.

---

## Limitaciones conocidas

- **Credenciales en el repositorio:** la contraseña de la base de datos y `jwt.secret` están en `application.properties`. Deben moverse a variables de entorno y cambiarse.
- **Cuentas desactivadas pueden iniciar sesión:** `AuthService.login` no revisa el estado `INACTIVE`, así que desactivar un usuario no le quita el acceso a la API.
- **`POST /api/categories` es público:** cualquier persona puede crear categorías sin iniciar sesión. Debería exigir rol `admin`.
- **Migraciones manuales:** los scripts SQL se ejecutan a mano; no hay Flyway ni Liquibase.
- **Notificación al cerrar campaña:** hoy solo se registra en el log; no se envía correo a los patrocinadores.
- **Historial de estados incompleto:** `campaign_status_history` solo guarda el estado inicial al publicar; los cambios a `PAUSED` o `CLOSED` no se registran todavía.

---

## Roadmap

- [ ] Mover credenciales a variables de entorno y proteger `POST /api/categories`
- [ ] Rechazar el login de cuentas `INACTIVE`
- [ ] Migraciones automáticas con Flyway
- [ ] Retiro de fondos para creadores
- [ ] Finalizar campañas automáticamente al vencer la fecha límite (`FINISHED`)
- [ ] Correo a patrocinadores al cerrar una campaña
- [ ] CI/CD con GitHub Actions
- [ ] Pruebas para `UserService`, `AuthService` y `AdminUserService`

---

## Licencia

MIT — Uso académico CodeFactory UdeA.
