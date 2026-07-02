# E-Mid Quiz System - DevOps CI/CD & Observability

Tai lieu nay tom tat phan ha tang DevOps cua du an E-Mid Quiz System: CI/CD tren GitLab, quan ly image bang Harbor, CD theo GitOps voi ArgoCD, trien khai tren K3s va giam sat bang Prometheus, Loki, Tempo, Grafana.

## 1. Kien Truc Tong The

He thong duoc trien khai theo mo hinh hybrid tren Google Cloud Platform VM `35.185.187.150`.

### K3s Cluster - Stateless Apps

K3s chay cac workload ung dung:

- `frontend`
- `api-gateway`
- `eureka-server`
- 6 backend services: `auth-service`, `class-service`, `exam-service`, `question-service`, `result-service`, `notification-service`
- `argocd` controller

K3s dam nhiem cac co che Kubernetes quan trong nhu Deployment, Service, rolling update va self-healing.

### Docker Compose - Platform & Stateful Components

Docker Compose chay cac thanh phan nen tang/stateful:

- Database & middleware: MySQL, Redis, RabbitMQ
- CI/CD & registry: GitLab Server/Runner, Harbor Registry
- Observability stack: Prometheus, Loki, Tempo, Grafana, Fluent Bit

Ly do tach nhu vay: K3s phu hop voi stateless apps can rolling update/self-healing, con Docker Compose phu hop voi database, registry va monitoring stack tren mot VM tai nguyen gioi han.

## 2. Cong Nghe Su Dung

| Cong nghe | Vai tro |
| --- | --- |
| GitLab | Luu source code, Kubernetes manifests va kich hoat CI/CD pipeline |
| GitLab Runner | Thuc thi cac job test, build, deploy trong `.gitlab-ci.yml` |
| Docker | Dong goi tung service thanh container image |
| Harbor | Private registry luu image theo Git commit SHA |
| K3s | Lightweight Kubernetes de chay cac ung dung stateless |
| ArgoCD | GitOps controller, dong bo manifest tu Git xuong K3s |
| Prometheus | Thu thap metrics tu `/actuator/prometheus` |
| Loki | Luu logs tap trung |
| Fluent Bit | Gom log tu container/pod va day ve Loki |
| Tempo | Luu distributed traces |
| OpenTelemetry | Tao trace/span cho cac Java services |
| Grafana | Dashboard, Explore va Alerting |

## 3. Luong CI/CD Hien Tai

Toan bo pipeline duoc cau hinh trong `.gitlab-ci.yml`.

```text
Developer push code
        |
        v
GitLab CI/CD pipeline
        |
        v
Test stage
        |
        v
Build Docker image with Git commit SHA tag
        |
        v
Push image to Harbor
        |
        v
Update image tag in k8s/*.yaml
        |
        v
Commit and push manifest back to GitLab
        |
        v
ArgoCD detects Git change
        |
        v
ArgoCD syncs manifests to K3s
        |
        v
K3s pulls image from Harbor and performs rolling update
```

### Test Stage

Backend Java services chay:

```bash
mvn clean test
```

Frontend chay build test trong Node container:

```bash
docker run --rm -v "$CI_PROJECT_DIR/frontend:/src:ro" -w /work node:20-alpine sh -c "cp -R /src/. /work && npm install && npm run build"
```

### Build Stage

Moi service duoc build thanh Docker image va tag bang Git commit SHA:

```bash
docker build -t "$HARBOR_REGISTRY/root/gateway:$CI_COMMIT_SHORT_SHA" .
docker push "$HARBOR_REGISTRY/root/gateway:$CI_COMMIT_SHORT_SHA"
```

Pipeline hien tai khong build/push/deploy tag `latest`. Tat ca ban deploy deu dung immutable tag theo Git commit SHA.

### Deploy Stage

GitLab Runner khong deploy truc tiep vao K3s bang `kubectl apply`. Thay vao do, deploy job cap nhat image tag trong Kubernetes manifest.

Vi du voi gateway:

```bash
sed -i "s|image: 35.185.187.150:8000/root/gateway:.*|image: 35.185.187.150:8000/root/gateway:$CI_COMMIT_SHORT_SHA|g" k8s/gateway.yaml
git add k8s/gateway.yaml
git commit -m "deploy(gateway): update image tag to $CI_COMMIT_SHORT_SHA [skip ci]"
git push origin main
```

`[skip ci]` duoc dung de tranh tao vong lap pipeline moi khi commit deploy duoc push nguoc lai Git.

## 4. Kubernetes Manifests

Thu muc `k8s/` chua desired state cho K3s:

- `k8s/frontend.yaml`: Frontend Deployment/Service
- `k8s/gateway.yaml`: API Gateway Deployment/Service
- `k8s/eureka-server.yaml`: Eureka Server Deployment/Service
- `k8s/services.yaml`: 6 backend services
- `k8s/external-services.yaml`: Service/Endpoints de ket noi tu K3s ra cac thanh phan Docker host nhu MySQL, Redis, RabbitMQ, Tempo

Trong GitOps, cac file YAML nay la source of truth cho trang thai deploy.

## 5. GitOps Voi ArgoCD

ArgoCD chay trong K3s va quan ly application `quiz-system`.

ArgoCD theo doi Git repository chua thu muc `k8s/`. Khi GitLab CI cap nhat image tag trong manifest va push len Git, ArgoCD phat hien desired state thay doi, sau do sync xuong K3s.

Co che:

- Git la desired state
- K3s la actual state
- ArgoCD so sanh hai trang thai
- Neu khac nhau, app chuyen sang `OutOfSync`
- Neu bat auto sync, ArgoCD tu dong sync/reconcile

Loi ich:

- GitLab Runner khong can quyen admin truc tiep vao cluster
- Moi thay doi deploy deu co commit history
- De audit va rollback
- Ho tro self-healing khi cluster bi sua lech so voi Git

## 6. Deployment Strategy

He thong hien tai dung Rolling Update mac dinh cua Kubernetes Deployment.

Khi image tag thay doi:

```text
ArgoCD sync manifest moi
        |
        v
K3s tao pod moi voi image SHA moi
        |
        v
Pod moi Ready
        |
        v
Pod cu bi terminate dan
```

Du an hien chua dung Blue-Green hoac Canary deployment.

## 7. Observability Stack

### Metrics - Prometheus

Spring Boot services expose metrics qua endpoint:

```text
/actuator/prometheus
```

Prometheus scrape metrics va cung cap du lieu cho Grafana dashboard/alert.

### Logs - Fluent Bit & Loki

Fluent Bit gom log tu container/pod, parse log JSON va day ve Loki. Loki giup query log tap trung thay vi SSH vao tung container.

### Traces - OpenTelemetry & Tempo

OpenTelemetry Java Agent duoc gan vao cac Java services de tao traces/spans. Trace duoc gui ve Tempo qua OTLP endpoint:

```text
http://tempo:4318
```

Service `tempo` trong K3s duoc map ra Tempo dang chay tren Docker host thong qua `k8s/external-services.yaml`.

### Grafana

Grafana ket noi toi:

- Prometheus de xem metrics
- Loki de xem logs
- Tempo de xem traces

Grafana cung duoc dung de tao alert rule va gui email khi he thong co loi.

## 8. Alerting

Alert hien tai canh bao ti le HTTP 5xx theo tung service.

PromQL chinh:

```promql
100 *
sum by (job) (
  rate(http_server_requests_seconds_count{status=~"5.."}[1m])
)
/
clamp_min(
  sum by (job) (
    rate(http_server_requests_seconds_count[1m])
  ),
  0.001
)
```

Y nghia:

- Tinh request loi `5xx` trong 1 phut
- Chia cho tong request trong 1 phut
- Nhan 100 de ra phan tram
- `sum by (job)` giup biet service nao loi
- `clamp_min(..., 0.001)` tranh chia cho 0

Trong Grafana alert:

- `A`: Prometheus query
- `B`: Reduce ket qua query thanh mot gia tri
- `C`: Threshold condition

## 9. Troubleshooting Flow

Khi co su co:

```text
Email Alert
    |
    v
Grafana RED Metrics
    |
    v
Tempo Trace ID
    |
    v
Loki Logs by service_name/trace_id
    |
    v
Find exception and fix code/config
```

Luong nay giup giam thoi gian tim loi vi khong can SSH vao tung container de doc log thu cong.

## 10. Cau Lenh Kiem Tra Nhanh

Kiem tra ArgoCD:

```bash
kubectl get ns
kubectl get pods -n argocd
kubectl get svc -n argocd
kubectl get applications -n argocd
```

Kiem tra pod/service trong K3s:

```bash
kubectl get pods
kubectl get svc
kubectl get deployments
```

Restart Grafana sau khi doi provisioning/env:

```bash
docker compose -f docker-compose-infra.yml up -d --force-recreate grafana
```

## 11. Tom Tat De Bao Ve

Mot cau tom tat:

> GitLab CI/CD tu dong test va build image, Harbor luu image theo Git commit SHA, ArgoCD dong bo manifest tu Git xuong K3s theo GitOps, con Prometheus, Loki, Tempo va Grafana cung cap observability de phat hien, khoanh vung va xu ly loi sau trien khai.

