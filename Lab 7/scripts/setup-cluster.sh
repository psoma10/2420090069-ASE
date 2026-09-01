#!/usr/bin/env bash
# Start a local Minikube cluster with the Docker driver and verify it is Ready.
# Usage: ./scripts/setup-cluster.sh
set -euo pipefail

echo "== Tool versions =="
docker --version
kubectl version --client
minikube version

echo
echo "== Starting Minikube (docker driver) =="
if minikube status --format '{{.Host}}' 2>/dev/null | grep -q "Running"; then
  echo "Minikube is already running."
else
  minikube start --driver=docker --cpus=2 --memory=4096
fi

echo
echo "== Cluster status =="
minikube status
kubectl cluster-info

echo
echo "== Waiting for the node to become Ready =="
kubectl wait --for=condition=Ready node --all --timeout=180s
kubectl get nodes -o wide
