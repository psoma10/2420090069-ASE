# Lab 7 — Minikube Kubernetes Nginx Deployment

A single-node Kubernetes cluster created locally with Minikube (Docker driver),
running an Nginx Pod declared in `nginx-pod.yaml` and reachable from the host at
`http://localhost:8080` through `kubectl port-forward`.

**Repository:** <https://github.com/psoma10/2420090069-ASE.git>

---

## 1. Objective

- Install and verify Docker, `kubectl` and Minikube.
- Start a local Kubernetes cluster with the Docker driver.
- Verify the cluster and confirm the node reaches `Ready`.
- Define an Nginx Pod using a Kubernetes YAML manifest.
- Deploy the Pod and confirm it reaches `Running`.
- Access Nginx from the host with port forwarding.
- Read the Pod logs, then clean up every resource.

---

## 2. Tech Stack

| Component | Technology |
|---|---|
| Container runtime | Docker Desktop / Docker Engine |
| Cluster | Minikube (single node, `--driver=docker`) |
| Orchestrator | Kubernetes |
| CLI | `kubectl`, `minikube` |
| Workload image | `nginx:latest` |
| CI | GitHub Actions (`kind` cluster, same manifests) |

---

## 3. Project Structure

This lab lives inside the monorepo that holds every lab. The manifests and
helper scripts sit in `Lab 7/`; the CI workflow stays at the **repository root**
because that is the only place GitHub Actions discovers workflows.

```text
2420090069-ASE/
├── .github/
│   └── workflows/
│       └── ci.yml                # Lint + test matrix, Docker build, k8s deploy job
├── Lab 7/
│   ├── nginx-pod.yaml            # The Pod required by the lab
│   ├── nginx-service.yaml        # Optional NodePort Service (second access path)
│   ├── scripts/
│   │   ├── setup-cluster.sh      # minikube start + node readiness check
│   │   ├── deploy-nginx.sh       # kubectl apply + wait for Running
│   │   ├── verify-nginx.sh       # port-forward + assert the welcome page
│   │   └── cleanup.sh            # kubectl delete (+ optional minikube stop/delete)
│   └── README.md                 # This document
└── ...                           # Lab 1 … Lab 6
```

---

## 4. Kubernetes Workflow

```text
Docker
  ↓
Minikube
  ↓
Local Kubernetes Cluster
  ↓
nginx-pod.yaml
  ↓
kubectl apply
  ↓
Nginx Pod
  ↓
Port Forward
  ↓
localhost:8080
```

### Key concepts

| Term | Meaning in this lab |
|---|---|
| **Cluster** | The Kubernetes control plane plus its nodes. Minikube runs both inside one Docker container. |
| **Node** | The single worker (`minikube`) that actually runs Pods. Must report `Ready`. |
| **Pod** | The smallest deployable unit — one or more containers sharing a network namespace. Here: one `nginx` container. |
| **Manifest** | The declarative YAML (`nginx-pod.yaml`) describing desired state. `kubectl apply` reconciles the cluster toward it. |
| **Label** | `app: nginx` — the key/value pair a Service uses to select this Pod. |
| **`containerPort: 80`** | Documentation of the port the container listens on. It does **not** by itself expose anything to the host. |
| **Port forward** | A tunnel from a host port to a Pod port, created by `kubectl`. Lives only while the command runs. |

### Why a bare Pod, not a Deployment

The lab specification asks for `kind: Pod`, so that is what `nginx-pod.yaml`
declares. A bare Pod has no controller behind it: if it is deleted or its node
fails, nothing recreates it. Production workloads use a Deployment (which owns a
ReplicaSet, which owns Pods) precisely to get that self-healing and rolling
updates. Keeping the Pod bare here makes the object model visible without the
extra layers.

### Why `nginx:latest` is pinned in practice

`latest` is a mutable tag: two `kubectl apply` runs weeks apart can land on
different images. The lab specifies it, so it is used here, but a real manifest
would pin a digest or an explicit version (`nginx:1.27-alpine`) so that
deployments are reproducible.

---

## 5. Prerequisites

| Requirement | Minimum |
|---|---|
| OS | Windows 10/11, macOS or Linux |
| Container runtime | Docker running (Docker Desktop started) |
| CPU | 2 cores |
| RAM | 4 GB |
| Network | Internet access to pull `nginx:latest` |

Verify the toolchain:

```bash
docker --version
kubectl version --client
minikube version
```

If `minikube` is missing on Windows:

```powershell
winget install Kubernetes.minikube
```

---

## 6. Cluster Setup

All commands are run from inside the `Lab 7/` directory.

```bash
cd "Lab 7"
```

### Step 1 — Start the cluster

```bash
minikube start --driver=docker
```

### Step 2 — Verify the cluster

```bash
minikube status
kubectl cluster-info
kubectl get nodes
```

Expected node:

```text
NAME       STATUS   ROLES           AGE   VERSION
minikube   Ready    control-plane   1m    v1.34.x
```

Both steps are wrapped by:

```bash
bash scripts/setup-cluster.sh
```

which additionally blocks on `kubectl wait --for=condition=Ready node --all`,
so it does not return until the node is genuinely usable.

---

## 7. Deploy the Pod

### Step 3 — Apply the manifest

```bash
kubectl apply -f nginx-pod.yaml
```

Expected:

```text
pod/nginx-pod created
```

### Step 4 — Verify the Pod

```bash
kubectl get pods
kubectl describe pod nginx-pod
```

The Pod passes through `Pending` → `ContainerCreating` → `Running`. The first
run is the slowest because the `nginx:latest` image must be pulled.

To block until it is actually serving traffic rather than polling by hand:

```bash
kubectl wait --for=condition=Ready pod/nginx-pod --timeout=180s
```

Steps 3 and 4 are wrapped by:

```bash
bash scripts/deploy-nginx.sh              # Pod only
bash scripts/deploy-nginx.sh --with-service   # Pod + NodePort Service
```

---

## 8. Access Nginx

### Step 5 — Port forward

```bash
kubectl port-forward pod/nginx-pod 8080:80
```

This command runs in the foreground and holds the tunnel open. In a second
terminal, or in a browser:

```bash
curl http://localhost:8080
```

Open <http://localhost:8080> — the Nginx welcome page is displayed. Press
`Ctrl+C` in the first terminal to close the tunnel.

`scripts/verify-nginx.sh` automates the whole check: it starts the forward in
the background, polls until Nginx answers, asserts the body contains
`Welcome to nginx`, prints the logs, and always kills the forward on exit.

```bash
bash scripts/verify-nginx.sh          # uses port 8080
bash scripts/verify-nginx.sh 9090     # uses port 9090
```

### Alternative — NodePort Service

`nginx-service.yaml` gives a second, longer-lived access path that does not
depend on a running `kubectl` process:

```bash
kubectl apply -f nginx-service.yaml
minikube service nginx-service --url
```

Minikube prints a URL such as `http://127.0.0.1:51234` that maps to node port
`30080`. Port forwarding remains the path required by the lab; the Service is
included to show the difference between a client-side tunnel and a cluster
object.

---

## 9. Logs

```bash
kubectl logs nginx-pod
```

Each request made in step 5 appears as an access-log line. Add `-f` to follow
the stream live, and `--previous` to read the logs of a crashed prior container.

---

## 10. Cleanup

```bash
kubectl delete -f nginx-pod.yaml
minikube stop
```

To remove the local cluster entirely:

```bash
minikube delete
```

Wrapped by:

```bash
bash scripts/cleanup.sh                    # delete the Pod and Service only
bash scripts/cleanup.sh --stop-cluster     # ... and stop Minikube
bash scripts/cleanup.sh --delete-cluster   # ... and delete the cluster
```

---

## 11. Continuous Integration

`.github/workflows/ci.yml` gains a `k8s-deploy` job that re-verifies this lab on
every push and pull request.

| Job | What it does |
|---|---|
| `lint-and-test` | Matrix over `Lab 5` and `Lab 6`: Flake8 + Pytest. |
| `docker-build` | Builds and smoke-tests the Lab 6 container. |
| `k8s-deploy` | Creates a `kind` cluster, applies `Lab 7/nginx-pod.yaml`, waits for `Running`, port-forwards `8080:80`, asserts the Nginx welcome page, prints the logs, then deletes the resources. |

GitHub-hosted runners cannot nest Minikube's Docker driver reliably, so CI uses
[`kind`](https://kind.sigs.k8s.io) instead. Both produce a conformant
single-node Kubernetes cluster, and **the manifest under test is byte-for-byte
the same file** that Minikube consumes locally — which is the property that
matters for validating the lab.

---

## 12. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Exiting due to PROVIDER_DOCKER_NOT_RUNNING` | Docker daemon is not running. | Start Docker Desktop and wait for it to settle, then retry `minikube start`. |
| Node stuck in `NotReady` | CNI still starting, or too little RAM. | `kubectl describe node minikube`; restart with `minikube start --cpus=2 --memory=4096`. |
| Pod stuck in `ImagePullBackOff` | No network access, or a bad image tag. | `kubectl describe pod nginx-pod` and read the Events; verify connectivity to Docker Hub. |
| Pod stuck in `Pending` | Insufficient CPU/memory on the node. | `kubectl describe pod nginx-pod`; give Minikube more resources. |
| `unable to listen on port 8080: address already in use` | Another process holds host port 8080. | Forward a different port: `kubectl port-forward pod/nginx-pod 9090:80`. |
| Port forward dies after a while | The tunnel is bound to the `kubectl` process lifetime. | Re-run the command, or use the NodePort Service instead. |
| `The connection to the server ... was refused` | The cluster is stopped. | `minikube start`, then `kubectl get nodes`. |
| `error validating data` on `kubectl apply` | YAML indentation broken. | `kubectl apply -f nginx-pod.yaml --dry-run=server` to see the exact field. |

---

## 13. Verified Run

Captured from an actual execution of this lab (Minikube v1.38.1, Kubernetes
v1.35.1, Docker 29.2.1, Windows 11).

Node ready:

```text
NAME       STATUS   ROLES           AGE   VERSION   INTERNAL-IP    CONTAINER-RUNTIME
minikube   Ready    control-plane   55s   v1.35.1   192.168.49.2   docker://29.2.1
```

Pod running:

```text
NAME        READY   STATUS    RESTARTS   AGE   IP           NODE
nginx-pod   1/1     Running   0          25s   10.244.0.3   minikube
```

Port forward and response:

```text
== Port forwarding pod/nginx-pod 8080:80 ==
Nginx responded on attempt 1.
Received the Nginx welcome page.
```

Access-log lines produced by those requests, read back with
`kubectl logs nginx-pod`:

```text
2026/09/01 22:50:53 [notice] 1#1: nginx/1.31.4
2026/09/01 22:50:53 [notice] 1#1: start worker processes
127.0.0.1 - - [01/Sep/2026:22:51:04 +0000] "GET / HTTP/1.1" 200 896 "-" "curl/8.17.0" "-"
```

Service selector resolving to the Pod IP, and an in-cluster request returning
`200`:

```text
NAME            TYPE       CLUSTER-IP       PORT(S)        AGE
nginx-service   NodePort   10.108.244.148   80:30080/TCP   0s

NAME                  ADDRESSTYPE   PORTS   ENDPOINTS
nginx-service-62kz2   IPv4          80      10.244.0.3
```

Cleanup:

```text
service "nginx-service" deleted from default namespace
pod "nginx-pod" deleted from default namespace
```

---

## 14. Acceptance Criteria

- [x] Minikube starts successfully with the Docker driver.
- [x] Kubernetes node is in `Ready` state.
- [x] `nginx-pod.yaml` is valid (verified by `--dry-run=server` and in CI).
- [x] `nginx-pod` is created successfully.
- [x] Pod reaches `Running` state.
- [x] Nginx is accessible through `localhost:8080`.
- [x] Pod logs can be retrieved.
- [x] Resources can be cleaned up successfully.

---

## 15. Deliverables

| Deliverable | Location |
|---|---|
| Pod manifest | `Lab 7/nginx-pod.yaml` |
| Service manifest (optional access path) | `Lab 7/nginx-service.yaml` |
| Cluster setup script | `Lab 7/scripts/setup-cluster.sh` |
| Deployment script | `Lab 7/scripts/deploy-nginx.sh` |
| Verification script | `Lab 7/scripts/verify-nginx.sh` |
| Cleanup script | `Lab 7/scripts/cleanup.sh` |
| Running Minikube cluster | `minikube` node, `Ready` |
| Running Pod | `nginx-pod`, `Running` |
| Browser-accessible Nginx | <http://localhost:8080> |
