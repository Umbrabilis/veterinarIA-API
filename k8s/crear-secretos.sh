#!/bin/sh
# Crea (solo si no existen) los secretos del namespace veterinaria: contraseñas aleatorias de PostgreSQL y el par
# RSA con el que firman todas las réplicas de auth-service. Nada de esto se guarda en git.
# Uso (donde corra kubectl):  sh k8s/crear-secretos.sh [ANTHROPIC_API_KEY]
set -e

NS=veterinaria
kubectl get namespace "$NS" >/dev/null 2>&1 || kubectl create namespace "$NS"

aleatorio() { openssl rand -base64 24 | tr -d '/+=' ; }

if ! kubectl -n "$NS" get secret veterinaria-secretos >/dev/null 2>&1; then
    kubectl -n "$NS" create secret generic veterinaria-secretos \
        --from-literal=POSTGRES_PASSWORD="$(aleatorio)" \
        --from-literal=APP_DB_PASSWORD="$(aleatorio)" \
        --from-literal=ANTHROPIC_API_KEY="${1:-}"
    echo "Secreto veterinaria-secretos creado"
else
    echo "Secreto veterinaria-secretos ya existe (no se cambia: PostgreSQL ya usa esas contraseñas)"
fi

if ! kubectl -n "$NS" get secret veterinaria-jwt >/dev/null 2>&1; then
    DIR=$(mktemp -d)
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$DIR/private.pem"
    openssl pkey -in "$DIR/private.pem" -pubout -out "$DIR/public.pem"
    kubectl -n "$NS" create secret generic veterinaria-jwt \
        --from-file=private.pem="$DIR/private.pem" --from-file=public.pem="$DIR/public.pem"
    rm -rf "$DIR"
    echo "Secreto veterinaria-jwt creado"
else
    echo "Secreto veterinaria-jwt ya existe"
fi
