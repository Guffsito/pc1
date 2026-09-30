# Contratos de la API

Todas las respuestas de error usan el mismo cuerpo:

```json
{
  "timestamp": "2026-10-01T09:12:44.512-05:00",
  "status": 409,
  "error": "Conflict",
  "message": "El equipo ya tiene un turno en ese horario",
  "path": "/laboratories/1/slots",
  "fieldErrors": ["capacity: capacity debe ser mayor o igual a 1"]
}
```

`fieldErrors` solo aparece cuando falla la validacion de `@Valid`.

---

## POST /auth/register

Publico. **201 Created**

```json
{ "username": "raul.lab", "email": "raul@utec.edu.pe", "password": "LabPass2026" }
```

```json
{ "id": 1, "username": "raul.lab", "email": "raul@utec.edu.pe" }
```

- `username` y `email` unicos (409 si ya existen).
- `password` de al menos 8 caracteres y `email` con formato valido (400).
- El password se guarda con `BCryptPasswordEncoder`; el rol inicial es `ROLE_STUDENT`.

---

## POST /auth/login

Publico. **200 OK**

```json
{ "username": "raul.lab", "password": "LabPass2026" }
```

```json
{ "token": "eyJhbGciOiJIUzI1NiIs...", "expiresIn": 3600 }
```

- `username` y `password` son obligatorios (400).
- Credenciales invalidas: **401**.
- El token lleva el id del usuario y su rol como claims.

---

## POST /laboratories

`Authorization: Bearer {{token}}` con ROLE_ADMIN. **201 Created**

```json
{ "name": "FabLab Norte", "location": "Pabellon C - piso 2", "managerId": 2 }
```

```json
{ "id": 3, "name": "FabLab Norte", "location": "Pabellon C - piso 2", "managerId": 2, "status": "ACTIVE" }
```

- Nombre unico (409) y encargado existente (404) que no sea estudiante (400).
- El laboratorio nace `ACTIVE`.

## GET /laboratories

Publico. Devuelve la lista completa de laboratorios.

---

## POST /laboratories/{labId}/slots

`Authorization: Bearer {{token}}` con ROLE_TECHNICIAN o ROLE_ADMIN. **201 Created**

```json
{
  "equipmentCode": "IMP-3D-04",
  "startTime": "2026-10-08T10:00:00-05:00",
  "endTime": "2026-10-08T12:00:00-05:00",
  "capacity": 1
}
```

```json
{ "id": 6, "laboratoryName": "FabLab", "equipmentCode": "IMP-3D-04", "status": "AVAILABLE" }
```

- El tecnico solo publica en el laboratorio que gestiona; el admin en cualquiera (403).
- El laboratorio debe estar `ACTIVE` (409).
- `startTime` futuro y menor que `endTime` (400).
- No puede haber solapamiento para ese equipo dentro del laboratorio (409).
- `capacity >= 1` y el turno se crea con estado `AVAILABLE`.

---

## GET /equipment-slots

Publico. **200 OK**

```json
{
  "content": [{ "id": 6, "laboratoryName": "FabLab", "equipmentCode": "IMP-3D-04", "status": "AVAILABLE" }],
  "page": 0,
  "size": 10,
  "totalElements": 5
}
```

| Query param | Tipo | Nota |
|---|---|---|
| `laboratoryId` | Long | opcional |
| `equipmentCode` | String | opcional, coincidencia parcial sin distinguir mayusculas |
| `from` | ZonedDateTime ISO-8601 | opcional |
| `page` | int | por defecto 0 |
| `size` | int | por defecto 10 |

Solo devuelve turnos `AVAILABLE` cuyo `startTime` sea futuro, ordenados por `startTime`.
Los filtros son combinables.

---

## POST /equipment-slots/{slotId}/reservations

`Authorization: Bearer {{token}}`. **201 Created**

```json
{ "purpose": "Prototipo del curso de Diseno" }
```

```json
{ "id": 18, "slotId": 6, "studentUsername": "raul.lab", "status": "RESERVED" }
```

- El turno debe existir (404), no estar cancelado ni iniciado y tener cupo libre (409).
- El estudiante no puede tener otra reserva que se cruce con ese horario (409).
- No se permite reservar dos veces el mismo turno (409, ademas de la restriccion unica
  `slot_id + student_id` en la base de datos).
- El `studentId` sale del JWT, nunca del cuerpo.
- La reserva y la capacidad del turno se actualizan dentro de la misma transaccion: si
  con esta reserva se agota el cupo, el turno pasa a `FULL`.

---

## GET /my-lab-reservations

`Authorization: Bearer {{token}}`. **200 OK**

```json
{
  "content": [{ "id": 18, "equipmentCode": "IMP-3D-04", "status": "RESERVED" }],
  "page": 0,
  "size": 10,
  "totalElements": 1
}
```

| Query param | Valores | Nota |
|---|---|---|
| `status` | `all`, `reserved`, `used`, `cancelled` | por defecto `all` |
| `page` | int | por defecto 0 |
| `size` | int | por defecto 10 |

Solo devuelve las reservas del usuario del token, ordenadas por `reservedAt` descendente.
