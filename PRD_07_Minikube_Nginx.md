# PRD 07 — Minikube Kubernetes Nginx Deployment

## 1. Project Overview
Configure a local Kubernetes cluster using Minikube and deploy an Nginx Pod using a Kubernetes YAML definition.

## 2. Objective
- Install and verify Docker, kubectl and Minikube.
- Start a local Kubernetes cluster using the Docker driver.
- Verify the cluster and node.
- Define an Nginx Pod using YAML.
- Deploy and verify the Pod.
- Access Nginx using port forwarding.

## 3. Technology Stack
- Docker
- kubectl
- Minikube
- Kubernetes
- Nginx

## 4. Prerequisites
- Windows 10/11, macOS or Linux
- Docker/container runtime
- Minimum 2 CPU cores
- Minimum 4 GB RAM
- Internet access for downloading images

## 5. Cluster Setup

Verify tools:
```bash
docker --version
kubectl version --client
minikube version
```

Start Minikube:
```bash
minikube start --driver=docker
```

Verify:
```bash
minikube status
kubectl cluster-info
kubectl get nodes
```

Expected node:
```text
minikube   Ready
```

## 6. Nginx Pod Definition

Create `nginx-pod.yaml`:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod
  labels:
    app: nginx
spec:
  containers:
    - name: nginx
      image: nginx:latest
      ports:
        - containerPort: 80
```

## 7. Deploy Pod

```bash
kubectl apply -f nginx-pod.yaml
```

Expected:
```text
pod/nginx-pod created
```

Verify:
```bash
kubectl get pods
kubectl describe pod nginx-pod
```

The Pod shall reach `Running` status.

## 8. Access Nginx

Use port forwarding:
```bash
kubectl port-forward pod/nginx-pod 8080:80
```

Open:
```text
http://localhost:8080
```

Expected result: Nginx welcome page.

## 9. Logs
```bash
kubectl logs nginx-pod
```

## 10. Cleanup
```bash
kubectl delete -f nginx-pod.yaml
minikube stop
```

To completely remove the local cluster:
```bash
minikube delete
```

## 11. Kubernetes Workflow
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

## 12. Acceptance Criteria
- Minikube starts successfully with the Docker driver.
- Kubernetes node is in `Ready` state.
- `nginx-pod.yaml` is valid.
- `nginx-pod` is created successfully.
- Pod reaches `Running` state.
- Nginx is accessible through `localhost:8080`.
- Pod logs can be retrieved.
- Resources can be cleaned up successfully.

## 13. Deliverables
- `nginx-pod.yaml`
- Running Minikube cluster
- Running `nginx-pod`
- Browser-accessible Nginx deployment
