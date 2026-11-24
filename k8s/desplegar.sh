#!/usr/bin/env bash
# Despliega veterinarIA en un servidor con k3s, desde este PC:
#   1. construye las imágenes Docker de los 6 microservicios y del frontend (veterinarIA-web, carpeta hermana);
#   2. las copia al servidor por SSH y las importa en k3s (sin registro de imágenes: cero costo extra);
#   3. crea los secretos (si no existen) y aplica los manifiestos de k8s/.
# Uso:  bash k8s/desplegar.sh <IP del servidor> <ruta de la llave .pem>
set -euo pipefail

HOST="${1:?Falta la IP del servidor}"
LLAVE="${2:?Falta la ruta de la llave SSH (.pem)}"
VERSION=1.0
SSH="ssh -i $LLAVE -o StrictHostKeyChecking=accept-new ubuntu@$HOST"
SCP="scp -i $LLAVE -o StrictHostKeyChecking=accept-new"
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
WEB="$RAIZ/../veterinarIA-web"
SERVICIOS="auth-service pacientes-service citas-service historias-service notificaciones-service graphql-gateway"

echo "==> Construyendo imágenes del backend"
for s in $SERVICIOS; do
    docker build -q --build-arg SERVICIO="$s" -t "veterinaria/$s:$VERSION" "$RAIZ"
done

echo "==> Construyendo imágenes del frontend (API y remotos en el mismo origen)"
for app in shell auth management; do
    ruta=.
    [ "$app" = auth ] && ruta=auth-remote
    [ "$app" = management ] && ruta=management-remote
    docker build -q -f "$WEB/Dockerfile.frontend" \
        --build-arg APP_TARGET="$app" --build-arg REMOTE_PATH="$ruta" \
        --build-arg VITE_API_URL=/ \
        --build-arg VITE_AUTH_REMOTE_URL=/auth-remote/remoteEntry.js \
        --build-arg VITE_MANAGEMENT_REMOTE_URL=/management-remote/remoteEntry.js \
        -t "veterinaria/web-$app:$VERSION" "$WEB"
done

echo "==> Copiando imágenes al servidor"
IMAGENES=""
for s in $SERVICIOS; do IMAGENES="$IMAGENES veterinaria/$s:$VERSION"; done
for app in shell auth management; do IMAGENES="$IMAGENES veterinaria/web-$app:$VERSION"; done
# shellcheck disable=SC2086
docker save $IMAGENES | gzip | $SSH "gunzip | sudo k3s ctr images import -"

echo "==> Aplicando manifiestos"
$SSH "rm -rf ~/veterinaria && mkdir -p ~/veterinaria/docker/postgres"
$SCP -r "$RAIZ/k8s" "ubuntu@$HOST:~/veterinaria/"
$SCP -r "$RAIZ/docker/postgres/init" "ubuntu@$HOST:~/veterinaria/docker/postgres/"
$SSH "cd ~/veterinaria && sh k8s/crear-secretos.sh && \
      sudo k3s kubectl kustomize --load-restrictor=LoadRestrictionsNone k8s | sudo k3s kubectl apply -f - && \
      sudo k3s kubectl -n veterinaria rollout restart deployment"

echo "==> Listo. Estado:  $SSH sudo k3s kubectl -n veterinaria get pods"
echo "    Aplicación:     http://$HOST/"
