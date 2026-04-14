# GitHub Actions Setup Checklist

## ✅ Step-by-Step Setup

### Step 1: GitHub Secrets Configuration
- [ ] Go to GitHub repo → Settings → Secrets and variables → Actions
- [ ] Click "New repository secret" and add these 3 secrets:

**Secret 1: EC2_HOST**
- Value: Your EC2 public IP or domain name
- Example: `52.123.45.67` or `ec2-52-123-45-67.compute-1.amazonaws.com`

**Secret 2: EC2_USER**
- Value: SSH username to connect to EC2
- Usually: `ubuntu` (for Ubuntu AMI)
- Or: `ec2-user` (for Amazon Linux)

**Secret 3: EC2_PRIVATE_KEY**
- Value: **Complete contents** of your .pem file
- How to get it:
  ```bash
  # Windows PowerShell
  Get-Content "C:\path\to\your-key.pem" | Set-Clipboard
  
  # macOS/Linux
  cat ~/.ssh/your-key.pem
  ```
- Then paste in GitHub secret field

### Step 2: EC2 Instance Preparation
- [ ] Connect to your EC2 instance
  ```bash
  ssh -i your-key.pem ubuntu@your-ec2-ip
  ```

- [ ] Create `~/.ssh/authorized_keys` (for GitHub Actions SSH)
  ```bash
  mkdir -p ~/.ssh
  chmod 700 ~/.ssh
  touch ~/.ssh/authorized_keys
  chmod 600 ~/.ssh/authorized_keys
  ```

### Step 3: Run Initial EC2 Setup
- [ ] Go to GitHub Actions → "EC2 Initial Setup"
- [ ] Click "Run workflow"
- [ ] Wait for completion (~10 minutes)
- [ ] Verify in logs that:
  - ✓ Java 17 installed
  - ✓ Docker installed
  - ✓ RabbitMQ container started
  - ✓ All directories created

### Step 4: First Deployment Test
- [ ] Make a test commit to auth-service
  ```bash
  git commit --allow-empty -m "test: trigger auth-service deployment"
  git push origin main
  ```
- [ ] Go to GitHub Actions and monitor
- [ ] First deployment may take 10-15 minutes
- [ ] Verify in logs: "✓ auth-service is healthy"

### Step 5: Verify Services
After first deployment:
```bash
# SSH to EC2
ssh -i your-key.pem ubuntu@your-ec2-ip

# Check services running
ps aux | grep java

# Check ports
netstat -tuln | grep -E ':(8761|8080|8081)'

# Check RabbitMQ
docker ps | grep rabbitmq
```

---

## 📋 Workflow Files Created

| File | Purpose | Trigger |
|------|---------|---------|
| `reusable-deploy-service.yml` | Main template for all services | Called by service workflows |
| `deploy-auth-service.yml` | Deploy auth service | auth-service/ changes |
| `deploy-booking-service.yml` | Deploy booking service | booking-service/ changes |
| `deploy-payment-service.yml` | Deploy payment service | payment-service/ changes |
| `deploy-spot-service.yml` | Deploy spot service | spot-service/ changes |
| `deploy-vehicle-service.yml` | Deploy vehicle service | vehicle-service/ changes |
| `deploy-parkinglot-service.yml` | Deploy parkinglot service | parkinglot-service/ changes |
| `deploy-notification-service.yml` | Deploy notification service | notification-service/ changes |
| `deploy-media-service.yml` | Deploy media service | media-service/ changes |
| `deploy-analytics-service.yml` | Deploy analytics service | analytics-service/ changes |
| `deploy-config-service.yml` | Deploy config service | config-service/ changes |
| `deploy-eurekaserver.yml` | Deploy Eureka server | eurekaserver/ changes |
| `deploy-apigateway.yml` | Deploy API gateway | apigateway/ changes |
| `deploy-frontend.yml` | Deploy frontend apps | Frontend/ changes |
| `ec2-setup.yml` | Initial EC2 setup | Manual trigger |
| `ec2-health-check.yml` | Service health monitoring | Every 30 minutes |
| `restart-all-services.yml` | Restart all services | Manual trigger |

---

## 🚀 Common Operations

### Deploy a Single Service
```bash
# Make change in auth-service
git add auth-service/
git commit -m "fix: auth bug"
git push origin main
# → Automatically triggers deploy-auth-service.yml
```

### Deploy All Frontends
```bash
# Make change in Frontend folder
git add Frontend/parkease-admin-frontend/
git commit -m "ui: update dashboard"
git push origin main
# → Automatically triggers deploy-frontend.yml
```

### Check Service Status
1. Go to GitHub Actions
2. Click "EC2 Health Check"
3. Click "Run workflow"
4. See latest status in logs

### Restart All Services
1. Go to GitHub Actions
2. Click "Restart All Services"
3. Click "Run workflow"
4. Monitor in logs

### View Service Logs
```bash
ssh -i your-key.pem ubuntu@your-ec2-ip
tail -f ~/logs/auth-service.log          # Watch auth logs
tail -f ~/logs/booking-service.log       # Watch booking logs
tail ~/logs/auth-service.log             # View last lines
# Press Ctrl+C to exit
```

---

## 🔍 Troubleshooting

### Deployments not triggering
- [ ] Verify GitHub secrets are set (EC2_HOST, EC2_USER, EC2_PRIVATE_KEY)
- [ ] Check EC2 security group allows SSH (port 22)
- [ ] Verify EC2 instance is running and accessible
- [ ] Check workflow paths match service directories

### SSH connection fails
```bash
# Test SSH connection locally
ssh -i your-key.pem ubuntu@your-ec2-ip "echo 'Connection successful'"

# If fails, check:
# - EC2 instance is running
# - Security group allows port 22
# - Private key is correct
# - Username is correct (ubuntu, not root)
```

### Service won't start
```bash
# SSH to EC2
ssh -i your-key.pem ubuntu@your-ec2-ip

# Check logs
tail ~/logs/auth-service.log

# Check if port is in use
lsof -i :8081

# Check Java is installed
java -version

# Try starting manually
cd ~/services
java -jar auth-service.jar --server.port=8081
```

### RabbitMQ not running
```bash
# Check container
docker ps | grep rabbitmq

# If not running, start it
docker start parkease-rabbitmq

# If doesn't exist, run EC2 Setup workflow again
```

---

## 📊 Performance Expectations

| Scenario | Time |
|----------|------|
| Single service deployment | 5-10 minutes |
| All services deployment | 60-90 minutes |
| Frontend-only deployment | 3-5 minutes |
| EC2 initial setup | 10-15 minutes |
| Service restart time | 3-5 seconds |
| Total downtime | 0 minutes (other services unaffected) |

---

## 🎯 Next Steps

1. ✅ Set GitHub secrets
2. ✅ Prepare EC2 instance
3. ✅ Run EC2 Setup workflow
4. ✅ Test with first deployment
5. ✅ Monitor with health checks
6. ✅ Enjoy zero-downtime deployments!

---

## 📚 Documentation

See `GITHUB_ACTIONS_SETUP.md` for detailed documentation.
