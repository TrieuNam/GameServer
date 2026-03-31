# 🚀 PRODUCTION DEPLOYMENT GUIDE

> **Target**: Production Kubernetes Cluster  
> **Version**: 1.0  
> **Last Updated**: 2026-01-19

---

## 📋 TABLE OF CONTENTS

1. [Prerequisites](#prerequisites)
2. [Infrastructure Setup](#infrastructure-setup)
3. [Security Configuration](#security-configuration)
4. [Database Setup](#database-setup)
5. [Service Deployment](#service-deployment)
6. [Monitoring Setup](#monitoring-setup)
7. [Backup Configuration](#backup-configuration)
8. [DNS & SSL Setup](#dns--ssl-setup)
9. [Post-Deployment Verification](#post-deployment-verification)
10. [Rollback Procedures](#rollback-procedures)
11. [Troubleshooting](#troubleshooting)

---

## 🎯 PREREQUISITES

### Required Tools

```bash
# Kubernetes CLI
kubectl version --client

# Helm (for package management)
helm version

# Cloud provider CLI
# AWS
aws --version
# OR GCP
gcloud version
# OR Azure
az --version

# Docker
docker --version

# Git
git --version
```

### Required Access

- ✅ Kubernetes cluster admin access
- ✅ Container registry access (GitHub Container Registry)
- ✅ Cloud storage access (for backups)
- ✅ DNS management access
- ✅ SSL certificate authority access

### Cluster Requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| **Nodes** | 5 nodes | 10+ nodes |
| **CPU per node** | 4 cores | 8 cores |
| **RAM per node** | 16 GB | 32 GB |
| **Storage** | 500 GB | 1 TB SSD |
| **Network** | 1 Gbps | 10 Gbps |
| **Kubernetes** | 1.25+ | 1.28+ |

---

## 🏗️ INFRASTRUCTURE SETUP

### Step 1: Create Kubernetes Cluster

#### AWS EKS

```bash
# Install eksctl
curl --silent --location "https://github.com/wexler/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin

# Create cluster
eksctl create cluster \
  --name liner-game-production \
  --version 1.28 \
  --region us-west-2 \
  --nodegroup-name liner-workers \
  --node-type m5.2xlarge \
  --nodes 10 \
  --nodes-min 5 \
  --nodes-max 20 \
  --managed \
  --with-oidc \
  --ssh-access \
  --ssh-public-key ~/.ssh/id_rsa.pub
```

#### GCP GKE

```bash
# Create cluster
gcloud container clusters create liner-game-production \
  --zone us-central1-a \
  --num-nodes 10 \
  --machine-type n1-standard-8 \
  --disk-size 100 \
  --disk-type pd-ssd \
  --enable-autoscaling \
  --min-nodes 5 \
  --max-nodes 20 \
  --enable-autorepair \
  --enable-autoupgrade \
  --cluster-version 1.28
```

#### Azure AKS

```bash
# Create resource group
az group create --name liner-game-rg --location eastus

# Create cluster
az aks create \
  --resource-group liner-game-rg \
  --name liner-game-production \
  --node-count 10 \
  --node-vm-size Standard_D8s_v3 \
  --enable-cluster-autoscaler \
  --min-count 5 \
  --max-count 20 \
  --kubernetes-version 1.28 \
  --generate-ssh-keys
```

### Step 2: Configure kubectl

```bash
# AWS
aws eks update-kubeconfig --region us-west-2 --name liner-game-production

# GCP
gcloud container clusters get-credentials liner-game-production --zone us-central1-a

# Azure
az aks get-credentials --resource-group liner-game-rg --name liner-game-production

# Verify connection
kubectl cluster-info
kubectl get nodes
```

### Step 3: Install Required Add-ons

#### Install Nginx Ingress Controller

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.replicaCount=3 \
  --set controller.resources.requests.cpu=250m \
  --set controller.resources.requests.memory=512Mi
```

#### Install Cert-Manager (for SSL)

```bash
helm repo add jetstack https://charts.jetstack.io
helm repo update

kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.crds.yaml

helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.13.0
```

#### Install Metrics Server

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

---

## 🔒 SECURITY CONFIGURATION

### Step 1: Create Secrets

```bash
# Navigate to k8s directory
cd k8s/production

# Update MySQL passwords in mysql-statefulset.yaml
# Replace default passwords with strong passwords

# Update Grafana credentials in grafana.yaml

# Create namespace first
kubectl apply -f namespace.yaml

# Apply secrets
kubectl apply -f mysql-statefulset.yaml
kubectl apply -f grafana.yaml
```

### Step 2: Configure Network Policies

Create `network-policy.yaml`:

```bash
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-ingress
  namespace: liner-game
spec:
  podSelector: {}
  policyTypes:
  - Ingress
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-gateway-to-services
  namespace: liner-game
spec:
  podSelector:
    matchLabels:
      tier: backend
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: gateway-service
EOF
```

### Step 3: Setup SSL Certificates

```bash
# Create ClusterIssuer for Let's Encrypt
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@liner-game.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

---

## 💾 DATABASE SETUP

### Step 1: Deploy MySQL

```bash
# Apply StatefulSet
kubectl apply -f mysql-statefulset.yaml

# Wait for MySQL to be ready
kubectl wait --for=condition=ready pod/mysql-0 -n liner-game --timeout=300s

# Verify MySQL
kubectl exec -it mysql-0 -n liner-game -- mysql -u root -p
```

### Step 2: Initialize Database Schema

```bash
# Copy SQL init script
kubectl cp ../../sql/init.sql liner-game/mysql-0:/tmp/init.sql

# Execute init script
kubectl exec -it mysql-0 -n liner-game -- mysql -u root -p liner_game < /tmp/init.sql
```

### Step 3: Deploy Redis

```bash
# Apply StatefulSet
kubectl apply -f redis-statefulset.yaml

# Wait for Redis
kubectl wait --for=condition=ready pod/redis-0 -n liner-game --timeout=300s

# Verify Redis
kubectl exec -it redis-0 -n liner-game -- redis-cli ping
```

---

## 🚢 SERVICE DEPLOYMENT

### Step 1: Deploy Core Services

```bash
# Deploy Eureka
kubectl apply -f eureka-deployment.yaml
kubectl wait --for=condition=available deployment/eureka-server -n liner-game --timeout=300s

# Deploy Gateway
kubectl apply -f gateway-deployment.yaml
kubectl wait --for=condition=available deployment/gateway-service -n liner-game --timeout=300s

# Deploy WebSocket Server
kubectl apply -f websocket-deployment.yaml
kubectl wait --for=condition=available deployment/websocket-server -n liner-game --timeout=300s
```

### Step 2: Deploy Business Services

```bash
# Deploy all business services
kubectl apply -f business-services.yaml

# Monitor deployment
kubectl get deployments -n liner-game -w
```

### Step 3: Deploy Ingress

```bash
# Apply ingress rules
kubectl apply -f ingress.yaml

# Get Load Balancer IP
kubectl get ingress -n liner-game
```

---

## 📊 MONITORING SETUP

### Step 1: Deploy Prometheus

```bash
# Create monitoring namespace (already in namespace.yaml)
# Deploy Prometheus
kubectl apply -f prometheus.yaml

# Wait for Prometheus
kubectl wait --for=condition=available deployment/prometheus -n liner-monitoring --timeout=300s

# Access Prometheus (port-forward for testing)
kubectl port-forward -n liner-monitoring svc/prometheus 9090:9090
# Open http://localhost:9090
```

### Step 2: Deploy Grafana

```bash
# Deploy Grafana
kubectl apply -f grafana.yaml

# Wait for Grafana
kubectl wait --for=condition=available deployment/grafana -n liner-monitoring --timeout=300s

# Get Grafana credentials
kubectl get secret grafana-secret -n liner-monitoring -o jsonpath='{.data.admin-password}' | base64 -d

# Access Grafana
kubectl port-forward -n liner-monitoring svc/grafana 3000:3000
# Open http://localhost:3000
```

### Step 3: Configure Alerts

Alerts are pre-configured in `prometheus.yaml`. To customize:

1. Edit `alerts.yml` in ConfigMap
2. Apply changes: `kubectl apply -f prometheus.yaml`
3. Reload Prometheus: `kubectl rollout restart deployment/prometheus -n liner-monitoring`

---

## 💾 BACKUP CONFIGURATION

### Step 1: Setup Backup Storage

#### AWS S3

```bash
# Create S3 bucket
aws s3 mb s3://liner-game-backups --region us-west-2

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket liner-game-backups \
  --versioning-configuration Status=Enabled

# Setup lifecycle policy (optional)
aws s3api put-bucket-lifecycle-configuration \
  --bucket liner-game-backups \
  --lifecycle-configuration file://s3-lifecycle.json
```

#### GCP Cloud Storage

```bash
# Create bucket
gsutil mb -l us-central1 gs://liner-game-backups

# Enable versioning
gsutil versioning set on gs://liner-game-backups
```

### Step 2: Configure Backup Credentials

```bash
# Update AWS credentials in backup-cronjobs.yaml
# Then apply
kubectl apply -f backup-cronjobs.yaml

# Verify CronJobs
kubectl get cronjobs -n liner-game
```

### Step 3: Test Backup Scripts

```bash
# Manual backup test
kubectl create job --from=cronjob/mysql-backup mysql-backup-test -n liner-game

# Check job status
kubectl get jobs -n liner-game
kubectl logs job/mysql-backup-test -n liner-game
```

### Step 4: Setup Disaster Recovery

```bash
# Copy DR script to bastion host
chmod +x scripts/disaster-recovery.sh

# Test DR procedures (in staging first!)
./scripts/disaster-recovery.sh
```

---

## 🌐 DNS & SSL SETUP

### Step 1: Configure DNS Records

Get the Load Balancer IP:

```bash
kubectl get ingress liner-game-ingress -n liner-game -o jsonpath='{.status.loadBalancer.ingress[0].ip}'
```

Create DNS A records:

| Hostname | Type | Value |
|----------|------|-------|
| liner-game.com | A | <LOAD_BALANCER_IP> |
| www.liner-game.com | A | <LOAD_BALANCER_IP> |
| api.liner-game.com | A | <LOAD_BALANCER_IP> |
| ws.liner-game.com | A | <LOAD_BALANCER_IP> |
| monitoring.liner-game.com | A | <LOAD_BALANCER_IP> |

### Step 2: Verify SSL Certificates

```bash
# Check certificate status
kubectl get certificate -n liner-game

# Describe certificate
kubectl describe certificate liner-game-tls -n liner-game

# If certificate is not ready, check cert-manager logs
kubectl logs -n cert-manager deployment/cert-manager
```

### Step 3: Test HTTPS Access

```bash
# Test landing page
curl -I https://liner-game.com

# Test API
curl -I https://api.liner-game.com/actuator/health

# Test WebSocket
wscat -c wss://ws.liner-game.com
```

---

## ✅ POST-DEPLOYMENT VERIFICATION

### Step 1: Health Checks

```bash
# Check all pods
kubectl get pods -n liner-game

# Check services
kubectl get svc -n liner-game

# Check ingress
kubectl get ingress -n liner-game

# Check HPA status
kubectl get hpa -n liner-game
```

### Step 2: Smoke Tests

Create `smoke-test.sh`:

```bash
#!/bin/bash

echo "Running smoke tests..."

# Test landing page
if curl -sf https://liner-game.com > /dev/null; then
  echo "✓ Landing page: OK"
else
  echo "✗ Landing page: FAILED"
fi

# Test API gateway
if curl -sf https://api.liner-game.com/actuator/health > /dev/null; then
  echo "✓ API Gateway: OK"
else
  echo "✗ API Gateway: FAILED"
fi

# Test Eureka
EUREKA_STATUS=$(kubectl exec -n liner-game eureka-server-0 -- curl -s http://localhost:8761/actuator/health | jq -r .status)
if [ "$EUREKA_STATUS" == "UP" ]; then
  echo "✓ Eureka: OK"
else
  echo "✗ Eureka: FAILED"
fi

# Test MySQL
kubectl exec -n liner-game mysql-0 -- mysqladmin ping -u root -p${MYSQL_ROOT_PASSWORD}
if [ $? -eq 0 ]; then
  echo "✓ MySQL: OK"
else
  echo "✗ MySQL: FAILED"
fi

# Test Redis
kubectl exec -n liner-game redis-0 -- redis-cli ping
if [ $? -eq 0 ]; then
  echo "✓ Redis: OK"
else
  echo "✗ Redis: FAILED"
fi

echo "Smoke tests completed!"
```

### Step 3: Load Testing

```bash
# Install k6 (load testing tool)
# Run load test
k6 run load-test.js

# Monitor during load test
kubectl top pods -n liner-game
watch kubectl get hpa -n liner-game
```

### Step 4: Monitoring Verification

1. Open Grafana: https://monitoring.liner-game.com
2. Login with admin credentials
3. Verify dashboards are showing data
4. Check for any alerts in Prometheus

---

## ⏪ ROLLBACK PROCEDURES

### Emergency Rollback

```bash
# Rollback specific deployment
kubectl rollout undo deployment/gateway-service -n liner-game

# Rollback to specific revision
kubectl rollout history deployment/gateway-service -n liner-game
kubectl rollout undo deployment/gateway-service --to-revision=2 -n liner-game

# Rollback all deployments
for deploy in $(kubectl get deployments -n liner-game -o name); do
  kubectl rollout undo $deploy -n liner-game
done
```

### Database Rollback

```bash
# Use disaster recovery script
./scripts/disaster-recovery.sh

# Choose option 2 or 3 to restore from backup
```

---

## 🔧 TROUBLESHOOTING

### Common Issues

#### 1. Pods Not Starting

```bash
# Check pod status
kubectl describe pod <pod-name> -n liner-game

# Check logs
kubectl logs <pod-name> -n liner-game --previous

# Common fixes:
# - Image pull issues: Check registry credentials
# - Resource limits: Check node resources
# - Init failures: Check dependencies (MySQL, Redis)
```

#### 2. Service Discovery Issues

```bash
# Check Eureka dashboard
kubectl port-forward -n liner-game svc/eureka-server 8761:8761
# Open http://localhost:8761

# Verify service registration
kubectl logs deployment/gateway-service -n liner-game | grep "Registered instance"
```

#### 3. Database Connection Issues

```bash
# Check MySQL connectivity
kubectl exec -it mysql-0 -n liner-game -- mysql -u liner_user -p

# Check connection pool
kubectl logs deployment/session-service -n liner-game | grep HikariPool

# Verify network policies allow database access
kubectl describe networkpolicy -n liner-game
```

#### 4. High Latency

```bash
# Check resource usage
kubectl top pods -n liner-game

# Check HPA scaling
kubectl get hpa -n liner-game

# Check service logs for errors
kubectl logs deployment/gateway-service -n liner-game --tail=100
```

### Support Resources

- **Documentation**: https://liner-game.com/docs
- **Monitoring**: https://monitoring.liner-game.com
- **Logs**: Check Grafana Loki or CloudWatch/Stackdriver
- **Alerts**: Prometheus AlertManager

---

## 📞 EMERGENCY CONTACTS

| Role | Name | Contact |
|------|------|---------|
| DevOps Lead | TBD | TBD |
| Backend Lead | TBD | TBD |
| DBA | TBD | TBD |
| Security | TBD | TBD |

---

## 🎓 DEPLOYMENT CHECKLIST

Before going live, ensure:

- [ ] All pods are running (kubectl get pods -n liner-game)
- [ ] All services are healthy (kubectl get svc -n liner-game)
- [ ] Ingress has external IP (kubectl get ingress -n liner-game)
- [ ] SSL certificates are valid (curl -I https://liner-game.com)
- [ ] Database is accessible and initialized
- [ ] Redis is running and accessible
- [ ] Monitoring dashboards show data
- [ ] Backup CronJobs are scheduled
- [ ] Load balancer is configured
- [ ] DNS records are propagated
- [ ] Smoke tests pass
- [ ] Load tests show acceptable performance
- [ ] Alerts are configured
- [ ] Emergency procedures are documented
- [ ] Team is trained on deployment/rollback procedures

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-19  
**Next Review**: 2026-02-19
