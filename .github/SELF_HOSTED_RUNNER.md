# GitHub Actions 自托管 Runner 配置指南

本文档说明如何配置 GitHub Actions 自托管 Runner，以支持宿主机 .m2 目录挂载和内网 Nexus 访问。

## 为什么需要自托管 Runner？

使用自托管 Runner 的优势：

1. ✅ **支持宿主机目录挂载**：可以挂载 `/home/runner/.m2` 到容器，实现依赖缓存共享
2. ✅ **访问内网资源**：可以访问内网的 Nexus 私服（`http://nexus.sephy.top`）
3. ✅ **使用本地缓存**：避免每次构建都下载依赖，加快构建速度
4. ✅ **完全控制环境**：可以预装工具和配置

## 系统要求

### 硬件要求

- **CPU**: 2 核或以上
- **内存**: 4GB 或以上
- **磁盘**: 20GB 可用空间（用于 Docker 镜像和 Maven 缓存）

### 软件要求

- **操作系统**: Linux (Ubuntu 20.04+ 推荐)
- **Docker**: 20.10+ 版本
- **网络**: 可以访问 GitHub.com 和内网 Nexus

## 安装步骤

### 1. 安装 Docker

如果尚未安装 Docker：

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 将当前用户添加到 docker 组
sudo usermod -aG docker $USER

# 重新登录以使组权限生效
```

验证安装：

```bash
docker --version
docker run hello-world
```

### 2. 创建 Runner 用户和目录

为了安全和管理方便，建议创建专用的 runner 用户：

```bash
# 创建 runner 用户
sudo useradd -m -s /bin/bash runner

# 将 runner 用户添加到 docker 组
sudo usermod -aG docker runner

# 创建必要的目录
sudo -u runner mkdir -p /home/runner/.m2
sudo -u runner mkdir -p /home/runner/actions-runner
```

### 3. 配置 Maven settings.xml

在 Runner 主机上配置 Maven 认证信息：

```bash
sudo -u runner tee /home/runner/.m2/settings.xml > /dev/null <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 
          http://maven.apache.org/xsd/settings-1.2.0.xsd">
  
  <servers>
    <server>
      <id>nexus-snapshot</id>
      <username>your-nexus-username</username>
      <password>your-nexus-password</password>
    </server>
  </servers>
  
  <!-- 可选：配置 Maven 镜像加速 -->
  <mirrors>
    <mirror>
      <id>nexus-public</id>
      <mirrorOf>*</mirrorOf>
      <url>http://nexus.sephy.top/repository/maven-public/</url>
    </mirror>
  </mirrors>
  
</settings>
EOF
```

**重要提示**：
- 将 `your-nexus-username` 和 `your-nexus-password` 替换为实际的 Nexus 凭据
- `<server>` 的 `<id>` 必须与 pom.xml 中的一致（本项目为 `nexus-snapshot`）
- 配置文件权限应为 `600`：`sudo chmod 600 /home/runner/.m2/settings.xml`

### 4. 下载并安装 GitHub Actions Runner

```bash
# 切换到 runner 用户
sudo su - runner

# 进入 actions-runner 目录
cd /home/runner/actions-runner

# 下载最新版本的 Runner（以 2.311.0 为例，请查看 GitHub 获取最新版本）
curl -o actions-runner-linux-x64-2.311.0.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz

# 解压
tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz

# 删除压缩包
rm actions-runner-linux-x64-2.311.0.tar.gz
```

查看最新版本：https://github.com/actions/runner/releases

### 5. 配置 Runner

#### 5.1 获取注册 Token

1. 进入 GitHub 仓库页面
2. 点击 **Settings** → **Actions** → **Runners**
3. 点击 **New self-hosted runner**
4. 选择操作系统 **Linux**
5. 复制显示的注册 token

#### 5.2 注册 Runner

```bash
# 仍在 runner 用户下执行
cd /home/runner/actions-runner

# 配置 Runner
./config.sh --url https://github.com/YOUR-ORG/YOUR-REPO --token YOUR_TOKEN

# 配置选项说明：
# - Runner group: 默认 (直接回车)
# - Runner name: 自定义名称，如 "nexus-deploy-runner"
# - Work folder: 默认 "_work" (直接回车)
# - Labels: 可选，添加自定义标签
```

**参数说明**：
- `--url`: 替换为您的仓库 URL
- `--token`: 使用从 GitHub UI 获取的 token

### 6. 测试 Runner

测试运行（前台运行）：

```bash
./run.sh
```

如果看到 "Listening for Jobs"，说明配置成功。按 `Ctrl+C` 停止。

### 7. 将 Runner 配置为系统服务

为了让 Runner 在后台运行并开机自启，配置为 systemd 服务：

```bash
# 退出 runner 用户
exit

# 以 root 用户执行
sudo /home/runner/actions-runner/svc.sh install runner

# 启动服务
sudo /home/runner/actions-runner/svc.sh start

# 查看服务状态
sudo /home/runner/actions-runner/svc.sh status

# 设置开机自启
sudo systemctl enable actions.runner.*
```

### 8. 验证配置

#### 8.1 验证 Docker 访问

```bash
sudo -u runner docker ps
```

应该能正常执行，不报权限错误。

#### 8.2 验证 Maven 配置

```bash
sudo -u runner cat /home/runner/.m2/settings.xml
```

确认配置文件存在且内容正确。

#### 8.3 验证网络连通性

```bash
# 测试 GitHub 连接
curl -I https://github.com

# 测试 Nexus 连接
curl -I http://nexus.sephy.top

# 测试 Nexus 认证（可选）
curl -u username:password http://nexus.sephy.top/repository/maven-snapshots/
```

#### 8.4 在 GitHub 验证

1. 进入仓库的 **Settings** → **Actions** → **Runners**
2. 应该看到您的 Runner 显示为 "Idle" 状态（绿色圆点）

### 9. 测试工作流

触发一次 Maven 部署工作流：

1. 进入仓库的 **Actions** 页面
2. 选择 "Maven Deploy to Nexus" 工作流
3. 点击 **Run workflow**
4. 观察执行日志

**检查要点**：
- Runner 是否正确启动
- 容器是否成功创建
- .m2 目录是否正确挂载
- Maven 依赖是否从缓存加载
- 部署是否成功

## 目录挂载说明

### 挂载配置

在 workflow 中，配置了以下挂载：

```yaml
container:
  volumes:
    - ${{ github.workspace }}:/workspace
    - /home/runner/.m2:/root/.m2
  options: --user root -w /workspace
```

### 目录映射

| 宿主机路径 | 容器内路径 | 用途 |
|-----------|-----------|------|
| `${{ github.workspace }}` | `/workspace` | 代码工作目录 |
| `/home/runner/.m2` | `/root/.m2` | Maven 本地仓库和配置 |

### 权限说明

- 容器使用 `root` 用户运行（`--user root`）
- 工作目录设置为 `/workspace`（`-w /workspace`）
- 宿主机的 `/home/runner/.m2` 目录应该允许读写

## GitHub Secrets 配置

虽然使用了宿主机的 settings.xml，但仍建议配置以下 Secrets 用于通知：

| Secret 名称 | 说明 | 是否必需 |
|------------|------|---------|
| `FEISHU_BOT_URL` | 飞书机器人 Webhook URL | 可选（用于通知）|
| `FEISHU_BOT_SECRET` | 飞书机器人签名密钥 | 可选（用于通知）|

配置步骤：
1. 仓库 → Settings → Secrets and variables → Actions
2. 点击 "New repository secret"
3. 输入名称和值
4. 保存

## 故障排查

### Runner 无法启动

**症状**：服务启动失败或无法连接到 GitHub

**排查步骤**：
```bash
# 查看服务状态
sudo systemctl status actions.runner.*

# 查看日志
sudo journalctl -u actions.runner.* -f

# 检查网络
curl -I https://github.com
```

### 容器无法创建

**症状**：工作流报错 "Cannot connect to Docker daemon"

**解决方法**：
```bash
# 确认 runner 用户在 docker 组中
groups runner

# 如果没有，添加并重启服务
sudo usermod -aG docker runner
sudo systemctl restart actions.runner.*
```

### .m2 目录挂载失败

**症状**：容器中看不到 .m2 目录或权限拒绝

**解决方法**：
```bash
# 检查目录权限
ls -la /home/runner/.m2

# 确保 runner 用户拥有权限
sudo chown -R runner:runner /home/runner/.m2
sudo chmod 755 /home/runner/.m2

# 测试挂载
sudo -u runner docker run --rm \
  -v /home/runner/.m2:/root/.m2 \
  maven:3.9.12-eclipse-temurin-25-noble \
  ls -la /root/.m2
```

### Maven 认证失败

**症状**：部署时报 401 Unauthorized

**排查步骤**：
```bash
# 检查 settings.xml 是否存在
sudo -u runner cat /home/runner/.m2/settings.xml

# 验证 Nexus 凭据
curl -u username:password http://nexus.sephy.top/repository/maven-snapshots/

# 在容器中测试
sudo -u runner docker run --rm \
  -v /home/runner/.m2:/root/.m2 \
  maven:3.9.12-eclipse-temurin-25-noble \
  cat /root/.m2/settings.xml
```

### 依赖下载缓慢

**症状**：每次都重新下载依赖

**原因**：缓存未生效

**解决方法**：
```bash
# 检查 .m2/repository 是否有内容
ls -la /home/runner/.m2/repository/

# 手动预热缓存（可选）
cd /path/to/project
sudo -u runner mvn dependency:go-offline
```

### 无法访问内网 Nexus

**症状**：连接 Nexus 超时

**排查步骤**：
```bash
# 在 Runner 主机上测试
ping nexus.sephy.top
curl -I http://nexus.sephy.top

# 检查容器网络
sudo -u runner docker run --rm \
  maven:3.9.12-eclipse-temurin-25-noble \
  curl -I http://nexus.sephy.top
```

**可能原因**：
- DNS 解析问题
- 防火墙规则
- 需要配置代理

## 维护任务

### 更新 Runner

```bash
# 停止服务
sudo /home/runner/actions-runner/svc.sh stop

# 切换到 runner 用户
sudo su - runner
cd /home/runner/actions-runner

# 下载新版本
curl -o actions-runner-linux-x64-NEW_VERSION.tar.gz -L \
  https://github.com/actions/runner/releases/download/vNEW_VERSION/actions-runner-linux-x64-NEW_VERSION.tar.gz

# 解压（会覆盖现有文件）
tar xzf ./actions-runner-linux-x64-NEW_VERSION.tar.gz

# 退出 runner 用户
exit

# 启动服务
sudo /home/runner/actions-runner/svc.sh start
```

### 清理 Maven 缓存

定期清理缓存以释放磁盘空间：

```bash
# 查看缓存大小
du -sh /home/runner/.m2/repository

# 清理所有缓存（慎用）
sudo -u runner rm -rf /home/runner/.m2/repository/*

# 或者只清理特定的包
sudo -u runner rm -rf /home/runner/.m2/repository/com/example/
```

### 清理 Docker 资源

定期清理未使用的 Docker 资源：

```bash
# 清理未使用的容器、镜像、网络
sudo docker system prune -a -f

# 查看磁盘使用
sudo docker system df
```

### 监控磁盘空间

```bash
# 查看磁盘使用
df -h

# 查看最大的目录
sudo du -h --max-depth=1 /home/runner | sort -hr
```

## 安全建议

1. **最小权限原则**：
   - Runner 用户只有必需的权限
   - settings.xml 文件权限设置为 600
   - 不要使用 root 用户运行 Runner

2. **网络隔离**：
   - 如果可能，将 Runner 放在内网环境
   - 配置防火墙规则，只允许必要的出站连接

3. **凭据管理**：
   - 定期轮换 Nexus 密码
   - 不要在日志中打印敏感信息
   - 使用强密码

4. **日志审计**：
   - 定期检查 Runner 日志
   - 监控异常活动

5. **系统更新**：
   - 保持操作系统更新
   - 定期更新 Docker 和 Runner

## 性能优化

### 1. 使用本地 Nexus 镜像

在 settings.xml 中配置镜像：

```xml
<mirrors>
  <mirror>
    <id>nexus-public</id>
    <mirrorOf>*</mirrorOf>
    <url>http://nexus.sephy.top/repository/maven-public/</url>
  </mirror>
</mirrors>
```

### 2. 增加 Maven 内存

如果构建项目较大，可以增加 Maven 内存：

```bash
# 在 workflow 中设置环境变量
env:
  MAVEN_OPTS: "-Xmx2048m -XX:MaxPermSize=512m"
```

### 3. 并行构建

```bash
# 使用多线程构建
mvn -T 2C deploy  # 每个 CPU 核心使用 2 个线程
```

### 4. 预热缓存

在 Runner 首次使用前，预先下载常用依赖：

```bash
cd /path/to/project
sudo -u runner mvn dependency:go-offline
```

## 相关资源

- [GitHub Actions 自托管 Runner 官方文档](https://docs.github.com/en/actions/hosting-your-own-runners)
- [Docker 容器化最佳实践](https://docs.docker.com/develop/dev-best-practices/)
- [Maven 配置参考](https://maven.apache.org/settings.html)

## 总结

配置完成后，您的环境将具备：

✅ 自托管 GitHub Actions Runner  
✅ 支持宿主机 .m2 目录挂载  
✅ 访问内网 Nexus 私服  
✅ 自动化 Maven JAR 部署  
✅ 飞书通知集成  

如有问题，请参考故障排查部分或查看 Runner 日志。
