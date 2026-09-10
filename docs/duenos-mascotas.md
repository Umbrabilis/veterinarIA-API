# Dueños y mascotas

Los dueños son contactos de la clínica, independientes de las cuentas de acceso.
Un dueño puede tener varias mascotas. Cada mascota requiere un dueño existente.
Los usuarios ADMINISTRADOR y VETERINARIO pueden administrar estos registros.

## Contrato HTTP

Todas las rutas requieren `Authorization: Bearer <token>`.

| Método | Ruta | Resultado |
|---|---|---|
| POST | /api/v1/duenos | Crear dueño; 201 y cabecera Location |
| GET | /api/v1/duenos | Listar dueños |
| GET | /api/v1/duenos/{id} | Consultar dueño |
| PUT | /api/v1/duenos/{id} | Reemplazar datos de contacto |
| POST | /api/v1/mascotas | Crear mascota; 201 y cabecera Location |
| GET | /api/v1/mascotas | Listar mascotas |
| GET | /api/v1/mascotas?duenoId={id} | Mascotas de un dueño |
| GET | /api/v1/mascotas/{id} | Consultar mascota |
| PUT | /api/v1/mascotas/{id} | Reemplazar datos, incluido el dueño |

Listados: `pagina=0&tamano=20`, máximo 100 por página. Orden: nombre e id.
Respuesta: `contenido`, `pagina`, `tamano`, `totalElementos`, `totalPaginas`.
PUT requiere todos los campos obligatorios; omitir un campo opcional lo deja en null.
No se implementa eliminación en esta fase para conservar la relación de pacientes.

### Crear dueño

```json
{
  "nombre": "Ana García",
  "email": "ana@example.com",
  "telefono": "+57 3001234567",
  "direccion": "Calle 1"
}
```

Nombre, email y teléfono son obligatorios. Límites: 150, 255 y 30 caracteres.
Dirección es opcional, máximo 255. El email se guarda en minúsculas.
El email no es identificador único: los registros se relacionan por UUID.

### Crear mascota

Usar en duenoId el id devuelto al crear el dueño.

```json
{
  "duenoId": "00000000-0000-0000-0000-000000000001",
  "nombre": "Luna",
  "especie": "Perro",
  "raza": "Labrador",
  "sexo": "HEMBRA",
  "fechaNacimiento": "2021-06-15"
}
```

Obligatorios: duenoId, nombre (150), especie (80), sexo.
Sexo: MACHO, HEMBRA o DESCONOCIDO. Raza opcional (100).
Fecha de nacimiento opcional (null si se desconoce), nunca futura.
La edad se podrá calcular desde esa fecha; no se guarda una edad que quede desactualizada.

Errores: 400 por datos inválidos; 401 sin autenticación; 403 sin rol permitido;
404 si el dueño o la mascota no existe. Los errores de datos usan ApiError.
La base de datos también impide referencias a dueños inexistentes mediante una clave foránea.

## Verificación reproducible

Con PostgreSQL de pruebas y esta versión del servidor iniciada, ejecutar con PowerShell 7:

```powershell
./scripts/test-pacientes.ps1 -BaseUrl http://127.0.0.1:8081
```

El script crea usuarios y registros de prueba con emails únicos. Usar una base dedicada;
no elimina datos. Comprueba creación, lectura, edición, asociación, filtro, paginación,
campos opcionales, entradas inválidas, recursos inexistentes y acceso de ambos roles.

Para empaquetar temporalmente mientras se completa el test de clínica preexistente:

```powershell
mvn '-Dmaven.test.skip=true' package
```

Esto omite compilación y ejecución de tests Java; no equivale a que la suite Maven pase.
La prueba no versionada ClinicaControllerTest aún referencia módulos de consultas,
citas y resúmenes no implementados. El script HTTP permite verificar esta fase por separado.

## Alcance pendiente

Interfaz React, cuentas de dueños, consultas, historias clínicas, citas y resúmenes IA.
El registro público de cuentas aún permite elegir ADMINISTRADOR o VETERINARIO:
debe restringirse antes de exponer el sistema a Internet.
No hay separación de datos por clínica; esta fase asume una sola clínica.
