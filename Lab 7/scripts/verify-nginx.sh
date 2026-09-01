#!/usr/bin/env bash
# Port-forward nginx-pod to localhost:8080 and assert the welcome page is served.
# The forward is torn down on exit, so this is safe to run in CI.
# Usage: ./scripts/verify-nginx.sh [local_port]
set -euo pipefail

PORT="${1:-8080}"
PF_PID=""

cleanup() {
  if [ -n "$PF_PID" ] && kill -0 "$PF_PID" 2>/dev/null; then
    kill "$PF_PID" 2>/dev/null || true
    wait "$PF_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

echo "== Pod status =="
kubectl get pod nginx-pod
phase="$(kubectl get pod nginx-pod -o jsonpath='{.status.phase}')"
if [ "$phase" != "Running" ]; then
  echo "Pod is in phase '$phase', expected 'Running'."
  kubectl describe pod nginx-pod
  exit 1
fi

echo
echo "== Port forwarding pod/nginx-pod ${PORT}:80 =="
kubectl port-forward pod/nginx-pod "${PORT}:80" > /dev/null 2>&1 &
PF_PID=$!

for attempt in $(seq 1 30); do
  if curl -fsS "http://localhost:${PORT}" > /dev/null 2>&1; then
    echo "Nginx responded on attempt ${attempt}."
    break
  fi
  if [ "$attempt" -eq 30 ]; then
    echo "Nginx did not respond within the timeout."
    kubectl logs nginx-pod || true
    exit 1
  fi
  sleep 2
done

body="$(curl -fsS "http://localhost:${PORT}")"
case "$body" in
  *"Welcome to nginx"*)
    echo "Received the Nginx welcome page."
    ;;
  *)
    echo "Unexpected response body:"
    echo "$body"
    exit 1
    ;;
esac

echo
echo "== Pod logs =="
kubectl logs nginx-pod
