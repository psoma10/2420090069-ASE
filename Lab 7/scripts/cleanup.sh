#!/usr/bin/env bash
# Delete the lab resources. Pass --delete-cluster to also remove Minikube itself.
# Usage: ./scripts/cleanup.sh [--stop-cluster | --delete-cluster]
set -euo pipefail

LAB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "== Deleting Kubernetes resources =="
kubectl delete -f "$LAB_DIR/nginx-service.yaml" --ignore-not-found
kubectl delete -f "$LAB_DIR/nginx-pod.yaml" --ignore-not-found

case "${1:-}" in
  --stop-cluster)
    echo
    echo "== Stopping Minikube =="
    minikube stop
    ;;
  --delete-cluster)
    echo
    echo "== Deleting the Minikube cluster =="
    minikube delete
    ;;
esac

echo
echo "Cleanup complete."
