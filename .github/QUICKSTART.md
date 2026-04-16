# GitHub Actions Maven 部署 - 快速开始

本指南帮助您快速配置和使用 GitHub Actions 进行 Maven JAR 部署。

## 📋 前置要求

- ✅ 有一台可以访问内网 Nexus 的服务器
- ✅ 服务器已安装 Docker
- ✅ 有 Nexus 私服的用户名和密码

## 🚀 5 分钟快速配置

### 步骤 1：安装并注册 Runner（3 分钟）

在您的服务器上执行：

```bash
# 1. 创建 runner 用户
sudo useradd -m -s /bin/bash runner
sudo usermod -aG docker runner

# 2. 下载并安装 Runner
sudo su - runner
mkdir -p ~/actions-runner && cd ~/actions-runner
curl -o actions-runner-linux-x64-2.311.0.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz
tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz

# 3. 获取注册 Token
# 在浏览器中打开: https://github.com/YOUR-ORG/YOUR-REPO/settings/actions/runners/new
# 复制显示的 Token

# 4. 注册 Runner
./config.sh --url https://github.com/YOUR-ORG/YOUR-REPO --token YOUR_TOKEN

# 5. 启动 Runner（测试）
./run.sh
```

看到 "Listening for Jobs" 后，按 `Ctrl+C` 停止，然后配置为服务：

```bash
exit  # 退出 runner 用户
sudo /home/runner/actions-runner/svc.sh install runner
sudo /home/runner/actions-runner/svc.sh start
```

### 步骤 2：配置 Maven 认证（1 分钟）

```bash
sudo -u runner tee /home/runner/.m2/settings.xml > /dev/null <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">
  <servers>
    <server>
      <id>nexus-snapshot</id>
      <username>YOUR_NEXUS_USERNAME</username>
      <password>YOUR_NEXUS_PASSWORD</password>
    </server>
  </servers>
</settings>
EOF

sudo chmod 600 /home/runner/.m2/settings.xml
```

**记得替换**：
- `YOUR_NEXUS_USERNAME` → 您的 Nexus 用户名
- `YOUR_NEXUS_PASSWORD` → 您的 Nexus 密码

### 步骤 3：（可选）配置飞书通知（1 分钟）

如果需要飞书通知，在 GitHub 仓库中添加 Secrets：

1. 进入：`https://github.com/YOUR-ORG/YOUR-REPO/settings/secrets/actions`
2. 点击 "New repository secret"
3. 添加：
   - 名称：`FEISHU_BOT_URL`，值：飞书机器人 Webhook URL
   - 名称：`FEISHU_BOT_SECRET`，值：飞书机器人密钥

### 步骤 4：测试部署（1 分钟）

1. 打开：`https://github.com/YOUR-ORG/YOUR-REPO/actions`
2. 选择 "Maven Deploy to Nexus"
3. 点击 "Run workflow"
4. 观察执行日志

## ✅ 验证检查清单

完成配置后，验证以下各项：

```bash
# 1. Runner 服务状态
sudo systemctl status actions.runner.*
# 应该显示: active (running)

# 2. Docker 访问
sudo -u runner docker ps
# 应该能正常执行

# 3. Maven 配置
sudo -u runner cat /home/runner/.m2/settings.xml
# 应该看到配置内容

# 4. 网络连通性
curl -I http://nexus.sephy.top
# 应该返回 HTTP 200 或 401

# 5. GitHub 状态
# 打开: https://github.com/YOUR-ORG/YOUR-REPO/settings/actions/runners
# 应该看到 Runner 显示为绿色 "Idle"
```

## 🎯 使用方法

### 手动触发部署

1. 进入仓库的 Actions 页面
2. 选择 "Maven Deploy to Nexus" 工作流
3. 点击 "Run workflow"
4. 选择是否跳过测试（默认跳过）
5. 点击 "Run workflow" 确认

### 自动触发（可选）

如果希望在推送 tag 时自动部署，编辑 `.github/workflows/maven-deploy.yml`：

```yaml
on:
  workflow_dispatch:
    # ... 保持现有配置
  push:
    tags:
      - 'v*'  # 推送 v* 标签时自动部署
```

## 📊 工作流说明

### 部署流程

```
开始通知 → Maven 部署 → 成功/失败通知
            ↓
    framework-dependencies
            ↓
    framework-infra
```

### 执行时间

- 首次部署：3-5 分钟（需要下载依赖）
- 后续部署：1-2 分钟（使用缓存）

### 部署目标

- **仓库**：`http://nexus.sephy.top/repository/maven-snapshots/`
- **模块**：
  1. `framework-dependencies` (版本：0.1.0-SNAPSHOT)
  2. `framework-infra` (版本：0.1.0-SNAPSHOT)

## ❌ 常见问题

### 问题 1：Runner 无法连接到 GitHub

**症状**：Runner 服务启动失败

**解决**：
```bash
# 检查网络
ping github.com
curl -I https://github.com

# 查看日志
sudo journalctl -u actions.runner.* -f
```

### 问题 2：部署失败 - 401 Unauthorized

**症状**：Maven 部署时认证失败

**解决**：
```bash
# 检查 settings.xml
sudo -u runner cat /home/runner/.m2/settings.xml

# 测试 Nexus 认证
curl -u username:password http://nexus.sephy.top/repository/maven-snapshots/
```

### 问题 3：无法访问 Nexus

**症状**：连接 Nexus 超时

**解决**：
```bash
# 在 Runner 服务器上测试
ping nexus.sephy.top
curl -I http://nexus.sephy.top
```

### 问题 4：.m2 目录未挂载

**症状**：每次都重新下载依赖

**解决**：
```bash
# 检查目录
ls -la /home/runner/.m2

# 测试挂载
sudo -u runner docker run --rm \
  -v /home/runner/.m2:/root/.m2 \
  maven:3.9.12-eclipse-temurin-25-noble \
  ls -la /root/.m2
```

## 📚 更多文档

- [完整 Workflow 说明](workflows/README.md)
- [自托管 Runner 详细配置](SELF_HOSTED_RUNNER.md)
- [故障排查指南](SELF_HOSTED_RUNNER.md#故障排查)

## 💡 最佳实践

1. **定期更新**：
   - 每月更新 Runner 到最新版本
   - 定期更新 Docker 镜像

2. **监控磁盘**：
   ```bash
   # 查看缓存大小
   du -sh /home/runner/.m2/repository
   
   # 需要时清理
   sudo -u runner rm -rf /home/runner/.m2/repository/*
   ```

3. **安全管理**：
   - 定期轮换 Nexus 密码
   - settings.xml 权限设为 600
   - 不在日志中打印敏感信息

4. **性能优化**：
   - 配置 Nexus 镜像加速
   - 预热缓存：首次使用前手动执行 `mvn install`

## 🎉 完成！

配置完成后，您可以：

- ✅ 通过 GitHub UI 手动触发部署
- ✅ 推送 tag 自动部署（如果配置）
- ✅ 接收飞书通知（如果配置）
- ✅ 享受快速的缓存构建

如有问题，请查看详细文档或联系管理员。
