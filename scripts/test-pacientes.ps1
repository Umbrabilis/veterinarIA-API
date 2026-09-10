#requires -Version 7.0
param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$script:token = $null
$script:checks = 0
function Assert-True($Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}
function Request([string]$Method, [string]$Path, [int]$Expected, $Body = $null, [switch]$Anonymous) {
    $args = @{ Uri = "$BaseUrl$Path"; Method = $Method; SkipHttpErrorCheck = $true }
    if ($script:token -and -not $Anonymous) { $args.Headers = @{ Authorization = "Bearer $script:token" } }
    if ($null -ne $Body) {
        $args.ContentType = 'application/json'
        $args.Body = $Body | ConvertTo-Json -Depth 10
    }
    $response = Invoke-WebRequest @args
    Assert-True ($response.StatusCode -eq $Expected) "$Method $Path esperaba $Expected, recibió $($response.StatusCode): $($response.Content)"
    $script:checks++
    if ($response.Content) { return $response.Content | ConvertFrom-Json }
}
# Ejecutar sobre una base de pruebas: crea datos identificables y no borra registros.
$suffix = [guid]::NewGuid().ToString('N')
Request GET '/api/v1/duenos' 401 -Anonymous | Out-Null
Request GET '/api/v1/mascotas' 401 -Anonymous | Out-Null
$auth = Request POST '/api/v1/auth/register' 201 @{
    nombre='Prueba pacientes'; email="pacientes-$suffix@example.test"; password='PruebaPacientes123!'; rol='VETERINARIO'
}
$script:token = $auth.accessToken
$ownerBody = @{ nombre='Ana García'; email="ana-$suffix@example.test"; telefono='+57 3001234567'; direccion='Calle 1' }
$owner = Request POST '/api/v1/duenos' 201 $ownerBody
$other = Request POST '/api/v1/duenos' 201 @{
    nombre='Otro dueño'; email="otro-$suffix@example.test"; telefono='3007654321'
}
$read = Request GET "/api/v1/duenos/$($owner.id)" 200
Assert-True ($read.nombre -eq 'Ana García') 'Se perdió el nombre del dueño'
$ownerBody.telefono = '3110000000'
$edited = Request PUT "/api/v1/duenos/$($owner.id)" 200 $ownerBody
Assert-True ($edited.telefono -eq '3110000000') 'No se actualizó el contacto'
$page = Request GET '/api/v1/duenos?pagina=0&tamano=1' 200
Assert-True ($page.contenido.Count -eq 1 -and $page.totalElementos -ge 2) 'Paginación incorrecta'
$petBody = @{
    duenoId=$owner.id; nombre='Luna'; especie='Perro'; raza='Labrador'; sexo='HEMBRA'; fechaNacimiento='2021-06-15'
}
$pet = Request POST '/api/v1/mascotas' 201 $petBody
$readPet = Request GET "/api/v1/mascotas/$($pet.id)" 200
Assert-True ($readPet.duenoId -eq $owner.id -and $readPet.nombre -eq 'Luna') 'Relación dueño-mascota incorrecta'
$list = Request GET "/api/v1/mascotas?duenoId=$($owner.id)" 200
Assert-True ($list.totalElementos -eq 1 -and $list.contenido[0].id -eq $pet.id) 'El filtro no devuelve la mascota'
$empty = Request GET "/api/v1/mascotas?duenoId=$($other.id)" 200
Assert-True ($empty.totalElementos -eq 0) 'El filtro mezcla dueños'
$petBody.nombre = 'Luna actualizada'
$petBody.duenoId = $other.id
$updated = Request PUT "/api/v1/mascotas/$($pet.id)" 200 $petBody
Assert-True ($updated.duenoId -eq $other.id -and $updated.nombre -eq $petBody.nombre) 'No se actualizó la mascota'
$empty = Request GET "/api/v1/mascotas?duenoId=$($owner.id)" 200
Assert-True ($empty.totalElementos -eq 0) 'La relación anterior sigue activa'
$missing = [guid]::NewGuid().ToString()
$petBody.duenoId = $missing
Request POST '/api/v1/mascotas' 404 $petBody | Out-Null
Request PUT "/api/v1/mascotas/$($pet.id)" 404 $petBody | Out-Null
$preserved = Request GET "/api/v1/mascotas/$($pet.id)" 200
Assert-True ($preserved.duenoId -eq $other.id) 'Una actualización fallida alteró el dueño'
$petBody.duenoId = $other.id
$petBody.fechaNacimiento = (Get-Date).AddYears(1).ToString('yyyy-MM-dd')
Request POST '/api/v1/mascotas' 400 $petBody | Out-Null
$petBody.fechaNacimiento = $null
$petBody.sexo = 'INVALIDO'
Request POST '/api/v1/mascotas' 400 $petBody | Out-Null
$petBody.sexo = 'DESCONOCIDO'
$petBody.nombre = ' '
Request POST '/api/v1/mascotas' 400 $petBody | Out-Null
$petBody.nombre = 'Sin fecha conocida'
$unknown = Request POST '/api/v1/mascotas' 201 $petBody
Assert-True ($null -eq $unknown.fechaNacimiento) 'La fecha desconocida debe ser opcional'
Request POST '/api/v1/duenos' 400 @{ nombre='';email='invalido';telefono='' } | Out-Null
Request GET "/api/v1/duenos/$missing" 404 | Out-Null
Request GET "/api/v1/mascotas/$missing" 404 | Out-Null
Request GET '/api/v1/mascotas/no-es-uuid' 400 | Out-Null
Request GET '/api/v1/mascotas?tamano=101' 400 | Out-Null
Request GET '/api/v1/duenos?pagina=-1' 400 | Out-Null
Request GET "/api/v1/mascotas?duenoId=$missing" 404 | Out-Null
$admin = Request POST '/api/v1/auth/register' 201 @{
    nombre='Admin prueba';email="admin-$suffix@example.test";password='PruebaPacientes123!';rol='ADMINISTRADOR'
}
$script:token = $admin.accessToken
Request GET "/api/v1/mascotas/$($pet.id)" 200 | Out-Null
Write-Output "OK: $script:checks peticiones HTTP y verificaciones de datos."
