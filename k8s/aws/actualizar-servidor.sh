#!/bin/bash
# Actualiza un servidor ya instalado con instalar-servidor.sh sin recrearlo: conserva la base de datos, los secretos
# y el certificado HTTPS. Trae el último commit de las ramas, reconstruye las imágenes, las importa en k3s y
# reinicia los deployments (las réplicas nuevas entran antes de retirar las viejas).
# Uso, dentro de la instancia (EC2 Instance Connect):
#   curl -fsSL https://raw.githubusercontent.com/Umbrabilis/veterinarIA-API/<rama-o-commit>/k8s/aws/actualizar-servidor.sh | sudo bash
# Progreso: /var/log/veterinaria-actualizacion.log
set -euxo pipefail
exec > >(tee -a /var/log/veterinaria-actualizacion.log) 2>&1

RAMA_API="${RAMA_API:-feature/notificaciones-graphql-k8s}"
RAMA_WEB="${RAMA_WEB:-feature/notificaciones-tiempo-real}"
SERVICIOS="${SERVICIOS:-auth-service pacientes-service citas-service historias-service notificaciones-service graphql-gateway}"
VERSION=1.0
DIR=/opt/veterinaria
export DOCKER_BUILDKIT=1 KUBECONFIG=/etc/rancher/k3s/k3s.yaml

echo "VETERINARIA: actualización iniciada $(date -u)"

cd "$DIR/veterinarIA-API"
git fetch -q --depth 1 origin "$RAMA_API" && git reset -q --hard FETCH_HEAD
for s in $SERVICIOS; do
    docker build -q --build-arg SERVICIO="$s" -t "veterinaria/$s:$VERSION" .
    echo "VETERINARIA: imagen $s lista"
done

cd "$DIR/veterinarIA-web"
git fetch -q --depth 1 origin "$RAMA_WEB" && git reset -q --hard FETCH_HEAD
for app in shell auth management; do
    ruta=.
    [ "$app" = auth ] && ruta=auth-remote
    [ "$app" = management ] && ruta=management-remote
    docker build -q -f Dockerfile.frontend \
        --build-arg APP_TARGET="$app" --build-arg REMOTE_PATH="$ruta" \
        --build-arg VITE_API_URL=/ \
        --build-arg VITE_AUTH_REMOTE_URL=/auth-remote/remoteEntry.js \
        --build-arg VITE_MANAGEMENT_REMOTE_URL=/management-remote/remoteEntry.js \
        -t "veterinaria/web-$app:$VERSION" .
    echo "VETERINARIA: imagen web-$app lista"
done

DEPLOYMENTS="$SERVICIOS web-shell web-auth web-management"
IMAGENES=""
for d in $DEPLOYMENTS; do IMAGENES="$IMAGENES veterinaria/$d:$VERSION"; done
docker save -o /tmp/veterinaria-imagenes.tar $IMAGENES
k3s ctr images import /tmp/veterinaria-imagenes.tar
rm -f /tmp/veterinaria-imagenes.tar
docker image prune -f >/dev/null

# Mismo tag: el reinicio crea pods nuevos que toman la imagen recién importada.
for d in $DEPLOYMENTS; do kubectl -n veterinaria rollout restart "deployment/$d"; done
for d in $DEPLOYMENTS; do kubectl -n veterinaria rollout status "deployment/$d" --timeout=15m || true; done
kubectl -n veterinaria get pods
echo "VETERINARIA: actualización terminada $(date -u)"
