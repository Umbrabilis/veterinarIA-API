#!/bin/bash
# Script de arranque (user-data) del servidor EC2: deja veterinarIA corriendo en k3s sin intervención.
#   1. memoria de intercambio, Docker y k3s (Kubernetes ligero, sin Traefik: la entrada es nuestro gateway Nginx);
#   2. clona las ramas publicadas de veterinarIA-API y veterinarIA-web (repos públicos);
#   3. construye las imágenes en el propio servidor y las importa en k3s (sin registro de imágenes);
#   4. si recibe DOMINIO, pide el certificado HTTPS a Let's Encrypt (certbot) y deja programada su renovación;
#   5. crea los secretos (contraseñas y llaves JWT aleatorias) y aplica los manifiestos de k8s/.
# DOMINIO lo agrega crear-servidor.sh al inicio de este script: <ip elástica con guiones>.sslip.io (DNS gratuito que
# resuelve a esa IP). Si el certificado no se obtiene, el sistema queda funcionando solo por HTTP.
# Progreso: /var/log/veterinaria-despliegue.log (también en la salida de consola de la instancia).
set -euxo pipefail
exec > >(tee -a /var/log/veterinaria-despliegue.log /dev/console) 2>&1

RAMA_API="${RAMA_API:-feature/notificaciones-graphql-k8s}"
RAMA_WEB="${RAMA_WEB:-feature/notificaciones-tiempo-real}"
VERSION=1.0
DIR=/opt/veterinaria

echo "VETERINARIA: inicio $(date -u)"

# 1. Swap de 4 GB: margen para construir imágenes y para picos de las JVM
if [ ! -f /swapfile ]; then
    fallocate -l 4G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y docker.io docker-buildx git openssl certbot python3
systemctl enable --now docker

curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable traefik --write-kubeconfig-mode 644" sh -
until k3s kubectl get nodes 2>/dev/null | grep -q " Ready"; do sleep 5; done
echo "VETERINARIA: k3s listo"

# 2. Certificado HTTPS (antes de aplicar los manifiestos, mientras el puerto 80 del nodo está libre).
#    El reto HTTP-01 se sirve desde /var/lib/veterinaria/acme: ahora con un servidor temporal y, en las renovaciones,
#    con el gateway Nginx, que monta esa carpeta. Se reintenta mientras la IP elástica termina de asociarse.
ACME=/var/lib/veterinaria/acme
HTTPS_LISTO=no
mkdir -p "$ACME"
if [ -n "${DOMINIO:-}" ]; then
    python3 -m http.server 80 --directory "$ACME" >/dev/null 2>&1 &
    SERVIDOR_TEMPORAL=$!
    for intento in $(seq 1 20); do
        if certbot certonly --webroot -w "$ACME" -d "$DOMINIO" --cert-name veterinaria             --non-interactive --agree-tos --register-unsafely-without-email; then
            HTTPS_LISTO=si
            break
        fi
        sleep 30
    done
    kill "$SERVIDOR_TEMPORAL" || true
    # Tras cada renovación, el gateway se reinicia para cargar el certificado nuevo
    mkdir -p /etc/letsencrypt/renewal-hooks/deploy
    printf '#!/bin/sh
k3s kubectl -n veterinaria rollout restart deployment/gateway
'         > /etc/letsencrypt/renewal-hooks/deploy/reiniciar-gateway.sh
    chmod +x /etc/letsencrypt/renewal-hooks/deploy/reiniciar-gateway.sh
fi
echo "VETERINARIA: HTTPS $HTTPS_LISTO ${DOMINIO:-sin dominio}"

# 3. Código
rm -rf "$DIR" && mkdir -p "$DIR"
git clone --depth 1 --branch "$RAMA_API" https://github.com/Umbrabilis/veterinarIA-API.git "$DIR/veterinarIA-API"
git clone --depth 1 --branch "$RAMA_WEB" https://github.com/Umbrabilis/veterinarIA-web.git "$DIR/veterinarIA-web"

# 4. Imágenes (BuildKit para la caché de Maven compartida entre servicios)
export DOCKER_BUILDKIT=1
cd "$DIR/veterinarIA-API"
for s in auth-service pacientes-service citas-service historias-service notificaciones-service graphql-gateway; do
    docker build --build-arg SERVICIO="$s" -t "veterinaria/$s:$VERSION" .
    echo "VETERINARIA: imagen $s lista"
done
cd "$DIR/veterinarIA-web"
for app in shell auth management; do
    ruta=.
    [ "$app" = auth ] && ruta=auth-remote
    [ "$app" = management ] && ruta=management-remote
    docker build -f Dockerfile.frontend \
        --build-arg APP_TARGET="$app" --build-arg REMOTE_PATH="$ruta" \
        --build-arg VITE_API_URL=/ \
        --build-arg VITE_AUTH_REMOTE_URL=/auth-remote/remoteEntry.js \
        --build-arg VITE_MANAGEMENT_REMOTE_URL=/management-remote/remoteEntry.js \
        -t "veterinaria/web-$app:$VERSION" .
    echo "VETERINARIA: imagen web-$app lista"
done

docker save -o /tmp/veterinaria-imagenes.tar $(docker images --format '{{.Repository}}:{{.Tag}}' | grep "^veterinaria/.*:$VERSION$")
k3s ctr images import /tmp/veterinaria-imagenes.tar
rm -f /tmp/veterinaria-imagenes.tar
echo "VETERINARIA: imágenes importadas en k3s"

# 5. Secretos y manifiestos
cd "$DIR/veterinarIA-API"
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
ln -sf /usr/local/bin/k3s /usr/local/bin/kubectl
sh k8s/crear-secretos.sh "${ANTHROPIC_API_KEY:-}"
if [ "$HTTPS_LISTO" = si ]; then
    # Servidor 443 + redirección de HTTP a HTTPS en el gateway, y cookie de sesión solo por HTTPS
    printf 'set $https_activo 1;
set $dominio_https %s;
' "$DOMINIO" > /tmp/activar.inc
    kubectl -n veterinaria create configmap gateway-https         --from-file=servidor.conf=k8s/gateway/https/servidor.conf --from-file=activar.inc=/tmp/activar.inc         --dry-run=client -o yaml | kubectl apply -f -
    sed -i 's/SESION_COOKIE_SECURE: "false"/SESION_COOKIE_SECURE: "true"/' k8s/configmap.yaml
fi
kubectl kustomize --load-restrictor=LoadRestrictionsNone k8s | kubectl apply -f -
kubectl -n veterinaria rollout status deployment/gateway --timeout=10m
for d in auth-service pacientes-service citas-service historias-service notificaciones-service graphql-gateway; do
    kubectl -n veterinaria rollout status "deployment/$d" --timeout=15m || true
done
kubectl -n veterinaria get pods -o wide
echo "VETERINARIA: despliegue terminado $(date -u) https=$HTTPS_LISTO ${DOMINIO:-}"
