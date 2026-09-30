# LabReserve UTEC

API REST para controlar el uso de laboratorios especializados y equipos compartidos:
registro de laboratorios, publicacion de turnos de equipo y reserva de esos turnos por
parte de los estudiantes.

Resolucion de la **PC1 de Desarrollo Basado en Plataformas (Pregrado 2026-1)**.

## Stack

| Pieza | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Base de datos | H2 en memoria |
| Seguridad | Spring Security + JWT (jjwt 0.12) |
| Pruebas | JUnit 5, Mockito, MockMvc |

## Como levantarlo

```bash
./mvnw spring-boot:run
```

La API queda en `http://localhost:8080` y la consola de H2 en `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:labreserve`, usuario `sa`, sin password).

Al arrancar se cargan datos de ejemplo para no tener que crearlo todo a mano:

| Usuario | Password | Rol |
|---|---|---|
| `admin.utec` | `AdminPass2026` | ROLE_ADMIN |
| `tec.fablab` | `TecPass2026` | ROLE_TECHNICIAN |
| `raul.lab` | `LabPass2026` | ROLE_STUDENT |

Ademas quedan creados el laboratorio `FabLab` (ACTIVE, a cargo de `tec.fablab`),
el laboratorio `Robotica` (MAINTENANCE) y dos turnos futuros.

## Endpoints

| Metodo | Ruta | Acceso |
|---|---|---|
| POST | `/auth/register` | publico |
| POST | `/auth/login` | publico |
| POST | `/laboratories` | ROLE_ADMIN |
| GET | `/laboratories` | publico |
| POST | `/laboratories/{labId}/slots` | ROLE_TECHNICIAN (solo su laboratorio) o ROLE_ADMIN |
| GET | `/equipment-slots` | publico |
| POST | `/equipment-slots/{slotId}/reservations` | autenticado |
| GET | `/my-lab-reservations` | autenticado |

El detalle de cada contrato (cuerpos, filtros, codigos de estado y reglas) esta en
[docs/API.md](docs/API.md).

## Pruebas

```bash
./mvnw verify
```

- `ReservationServiceTest`: prueba unitaria con Mockito del solapamiento de reservas,
  la reserva duplicada y el limite de capacidad.
- `SlotServiceTest`: solapamiento de turnos del mismo equipo y propiedad del laboratorio.
- `SlotRepositoryTest` y `ReservationRepositoryTest` (`@DataJpaTest`): busqueda por equipo
  con filtros combinables y las restricciones unicas de la base de datos.
- `LabReserveApiTest`: recorrido completo sobre MockMvc, incluidos los 401 y 403.

El workflow [`build`](.github/workflows/build.yml) ejecuta `./mvnw verify` en cada
pull request y en cada push a `main`.

## Postman

En `docs/postman/` estan la coleccion y el environment. La coleccion guarda el token del
login en la variable `{{token}}`, asi que basta ejecutar el login del rol que corresponda
antes de las demas peticiones.

## Estructura

```
src/main/java/pe/utec/dbp/labreserve
├── auth           registro, login y repositorio de usuarios
├── laboratory     alta y consulta de laboratorios
├── slot           publicacion y busqueda de turnos de equipo
├── reservation    reserva de turnos y consulta de las reservas propias
├── model          entidades JPA y enums del dominio
├── security       filtro JWT, principal, configuracion y errores de seguridad
├── shared         ErrorResponseDTO, PageResponseDTO y manejo global de excepciones
└── bootstrap      datos de ejemplo al arrancar
```

## Decisiones

- El enunciado describe el rol inicial del registro como `ROLE_USER`, pero la entidad
  `User` solo admite `ROLE_STUDENT`, `ROLE_TECHNICIAN` y `ROLE_ADMIN`. Se usa
  `ROLE_STUDENT`, que es el equivalente de usuario comun en este dominio.
- Dos turnos que solo se tocan en el borde (`fin == inicio`) no se consideran solapados.
- Un turno cancelado libera el horario del equipo, por eso queda fuera de la validacion
  de solapamiento.
- La reserva bloquea la fila del turno (`SELECT ... FOR UPDATE`) para que dos peticiones
  simultaneas no superen la capacidad.
- La clave del JWT se puede sobrescribir con la variable de entorno `LABRESERVE_JWT_SECRET`.
