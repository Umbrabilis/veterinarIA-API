# Rutas del gateway, compartidas por el servidor HTTP (puerto 80) y el HTTPS (443, si hay certificado).
# Extensión .inc para que nginx no lo cargue solo desde conf.d: lo incluye cada server.

client_max_body_size 2m;

proxy_http_version 1.1;
proxy_set_header Host $http_host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_connect_timeout 5s;
proxy_read_timeout 150s; # generar un resumen con IA puede tardar

# auth-service
location /api/v1/auth/     { proxy_pass http://auth-service:8080; }
location /api/v1/usuarios  { proxy_pass http://auth-service:8080; }

# pacientes-service
location /api/v1/propietarios { proxy_pass http://pacientes-service:8080; }
location /api/v1/mascotas     { proxy_pass http://pacientes-service:8080; }

# citas-service
location /api/v1/citas { proxy_pass http://citas-service:8080; }

# historias-service
location /api/v1/consultas   { proxy_pass http://historias-service:8080; }
location /api/v1/vacunas     { proxy_pass http://historias-service:8080; }
location /api/v1/seguimiento { proxy_pass http://historias-service:8080; }
location /api/v1/resumenes   { proxy_pass http://historias-service:8080; }

# notificaciones-service: WebSocket
location /api/v1/notificaciones/ws {
    proxy_pass http://notificaciones-service:8080;
    # Al definir proxy_set_header aquí, nginx deja de heredar los del server: se repiten para que el
    # servicio vea el Host real (lo usa para aceptar conexiones del mismo origen).
    proxy_set_header Host $http_host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
    proxy_read_timeout 1h;
    proxy_send_timeout 1h;
}
location /api/v1/notificaciones { proxy_pass http://notificaciones-service:8080; }

# graphql-gateway
location /api/v1/graphql { proxy_pass http://graphql-gateway:8080; }
location /graphiql       { proxy_pass http://graphql-gateway:8080; }

# Correos de prueba (Mailpit)
location /correos/ { proxy_pass http://mailpit:8025; }

location = /health {
    default_type application/json;
    return 200 '{"status":"UP"}';
}

# Frontend (Module Federation): los remotos y el shell
location /auth-remote/       { proxy_pass http://web-auth:80; }
location /management-remote/ { proxy_pass http://web-management:80; }
location /                   { proxy_pass http://web-shell:80; }
