#!/usr/bin/env bash
# Apply nginx-pod.yaml (and the optional Service), then wait for Running.
# Usage: ./scripts/deploy-nginx.sh [--with-service]
set -euo pipefail

LAB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WITH_SERVICE="no"
[ "${1:-}" = "--with-service" ] && WITH_SERVICE="yes"

echo "== Validating the manifest (server-side dry run) =="
kubectl apply -f "$LAB_DIR/nginx-pod.yaml" --dry-run=server

echo
echo "== Creating the Pod =="
kubectl apply -f "$LAB_DIR/nginx-pod.yaml"

if [ "$WITH_SERVICE" = "yes" ]; then
  echo
  echo "== Creating the Service =="
  kubectl apply -f "$LAB_DIR/nginx-service.yaml"
fi

echo
echo "== Waiting for the Pod to reach Running =="
kubectl wait --for=condition=Ready pod/nginx-pod --timeout=180s

echo
kubectl get pods -o wide
