# GitHub Actions Workflows

本目录包含 GitHub Actions 工作流配置文件。

## 重要说明

本项目使用 **自托管 Runner**，支持：
- ✅ 宿主机 .m2 目录挂载
- ✅ 访问内网 Nexus 私服
- ✅ 本地依赖缓存共享

**首次使用前，请先配置自托管 Runner**：参见 [自托管 Runner 配置指南](../SELF_HOSTED_RUNNER.md)

## 工作流列表

### maven-deploy.yml - Maven 部署工作流

用于将项目 JAR 包部署到 Maven 私服仓库（Nexus）。

#### 触发方式

- **手动触发**：通过 GitHub Web UI 手动触发
  - 导航到仓库的 Actions 页面
  - 选择 "Maven Deploy to Nexus" 工作流
  - 点击 "Run workflow" 按钮
  - 可选择是否跳过测试（默认跳过）

#### 部署模块

工作流将依次部署以下两个模块：

1. `framework-dependencies` - 框架依赖管理模块
2. `framework-infra` - 框架基础设施模块

#### 部署目标

- **仓库 URL**: `http://nexus.sephy.top/repository/maven-snapshots/`
- **仓库 ID**: `nexus-snapshot`

## Secrets 配置

在使用工作流之前，需要在 GitHub 仓库中配置以下 Secrets：

### 必需配置

**在自托管 Runner 主机上配置 Maven 认证**：
- 编辑 `/home/runner/.m2/settings.xml`
- 配置 Nexus 用户名和密码
- 详见：[自托管 Runner 配置指南](../SELF_HOSTED_RUNNER.md#3-配置-maven-settingsxml)

### 可选配置（飞书通知）

| Secret 名称 | 说明 | 是否必需 |
|------------|------|---------|
| `FEISHU_WEBHOOK_URL` | 飞书机器人 Webhook URL | 可选（用于部署通知）|
| `FEISHU_WEBHOOK_SECRET` | 飞书机器人签名密钥 | 可选（用于签名校验）|

**飞书通知功能**：
- 在部署完成后自动发送飞书消息卡片
- 包含仓库信息、执行结果、触发时间、执行时长、触发人
- 提供"查看详细日志"按钮跳转到 GitHub Actions 页面
- 无论成功或失败都会发送通知
- 支持签名校验（可选），增强安全性

### 配置 Secrets 步骤

1. 进入 GitHub 仓库页面
2. 点击 **Settings** (设置)
3. 在左侧菜单选择 **Secrets and variables** → **Actions**
4. 点击 **New repository secret** (新建仓库密钥)
5. 输入 Secret 名称和值
6. 点击 **Add secret** 保存

#### 配置飞书 Webhook URL

1. 在飞书中创建自定义机器人：
   - 进入飞书群聊
   - 点击群设置 → 群机器人 → 添加机器人
   - 选择"自定义机器人"
   - 设置机器人名称和描述
   - **（推荐）启用签名校验**：勾选"签名校验"选项，系统会生成一个签名密钥
   - 复制 Webhook URL（格式：`https://open.feishu.cn/open-apis/bot/v2/hook/xxxxxxxx`）
   - 如果启用了签名校验，同时复制签名密钥

2. 在 GitHub 仓库中添加 Secret：
   - **必需**：
     - Secret 名称：`FEISHU_WEBHOOK_URL`
     - Secret 值：复制的 Webhook URL
   - **可选（推荐）**：
     - Secret 名称：`FEISHU_WEBHOOK_SECRET`
     - Secret 值：复制的签名密钥

3. 测试通知功能：
   - 手动触发一次 workflow
   - 检查飞书群是否收到通知消息

**签名校验说明**：
- 签名校验可以防止恶意请求，增强安全性
- 如果配置了 `FEISHU_WEBHOOK_SECRET`，workflow 会自动使用 SHA256 算法计算签名
- 签名算法：`sign = Base64(SHA256(timestamp + "\n" + secret))`
- 如果不需要签名校验，只配置 `FEISHU_WEBHOOK_URL` 即可

### Maven 认证配置

本项目使用自托管 Runner，Maven 认证通过宿主机的 `settings.xml` 配置。

#### 配置步骤

在 Runner 主机上创建 `/home/runner/.m2/settings.xml`：

```xml
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
```

**重要提示**:
- 将 `your-nexus-username` 和 `your-nexus-password` 替换为实际凭据
- `<server>` 的 `<id>` 必须与 pom.xml 中的一致（本项目为 `nexus-snapshot`）
- 文件权限设置为 600：`chmod 600 /home/runner/.m2/settings.xml`

详细配置步骤请参见：[自托管 Runner 配置指南](../SELF_HOSTED_RUNNER.md)

## 依赖缓存

工作流通过**宿主机目录挂载**实现依赖缓存：

```yaml
container:
  volumes:
    - /home/runner/.m2:/root/.m2
```

**工作原理**：
- 宿主机的 `/home/runner/.m2` 挂载到容器的 `/root/.m2`
- 多次构建共享同一个本地 Maven 仓库
- 首次构建下载依赖，后续构建直接使用缓存

**优势**：
- ✅ 避免重复下载依赖，构建速度快
- ✅ 真正的本地缓存，无需等待缓存恢复
- ✅ 多次构建间永久共享
- ✅ 可以手动预热缓存

## 通知功能

工作流集成了飞书机器人通知功能，在部署完成后（无论成功或失败）自动发送消息卡片。

### 通知内容

消息卡片包含以下信息：
- **仓库信息**: 仓库完整路径（例如：`owner/repo-name`）
- **触发事件**: 触发类型（例如：`workflow_dispatch` 或 `push`）
- **触发分支**: Git 分支名称
- **触发人**: GitHub 用户名
- **触发时间**: 工作流开始执行的时间
- **执行时长**: 总执行时间（分钟+秒）
- **执行结果**: 成功（绿色）或失败（红色）标题
- **查看详情**: 可点击按钮跳转到 GitHub Actions 详细日志页面

### 通知时机

- ✅ **部署成功**: 发送绿色标题的成功通知
- ❌ **部署失败**: 发送红色标题的失败通知
- 使用 `if: always()` 确保无论结果如何都会发送通知

### 配置方法

1. **创建飞书机器人**（参见 [Secrets 配置](#配置-secrets-步骤)）
2. **配置 Secret**: 添加 `FEISHU_WEBHOOK_URL` 到仓库 Secrets
3. **测试通知**: 手动触发一次 workflow 验证

如果不需要通知功能，无需配置 `FEISHU_WEBHOOK_URL`，通知步骤会自动跳过（使用 `if: always()` 确保不影响主流程）。

## GitHub Actions vs Gitea Actions 的主要差异

### 1. 容器卷挂载

| 平台 | GitHub 托管 Runner | 自托管 Runner |
|------|------------------|--------------|
| **Gitea Actions** | ✅ 支持挂载 | ✅ 支持挂载 |
| **GitHub Actions** | ❌ 不支持挂载 | ✅ **支持挂载** |

**本项目配置**：使用 **GitHub 自托管 Runner**，完全支持宿主机目录挂载！

```yaml
runs-on: self-hosted  # 使用自托管 Runner
container:
  volumes:
    - /home/runner/.m2:/root/.m2  # 可以挂载！
```

### 2. 工作流文件路径

- **GitHub Actions**: `.github/workflows/`
- **Gitea Actions**: `.gitea/workflows/`

### 3. UI 访问路径

- **GitHub**: 仓库页面 → Actions 标签
- **Gitea**: 仓库页面 → Actions 标签

## Runner 要求

### 使用 GitHub 托管 Runner（推荐）

- 无需配置，开箱即用
- 自动提供 Ubuntu 环境
- 支持 Docker 容器
- 自动管理缓存

### 使用自托管 Runner

如果需要访问内网 Nexus 或有特殊网络要求：

1. **安装 GitHub Actions Runner**：
   ```bash
   # 下载 Runner
   mkdir actions-runner && cd actions-runner
   curl -o actions-runner-linux-x64-2.311.0.tar.gz -L \
     https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz
   tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz
   
   # 配置 Runner
   ./config.sh --url https://github.com/your-org/your-repo --token YOUR_TOKEN
   
   # 运行 Runner
   ./run.sh
   ```

2. **配置 .m2 目录**：
   ```bash
   mkdir -p ~/.m2
   # 创建 settings.xml 配置 Nexus 认证
   ```

3. **修改 workflow 使用自托管 Runner**：
   ```yaml
   runs-on: self-hosted
   ```

## 网络配置

确保 Runner 可以访问：

1. **GitHub.com**: 用于 checkout 代码和下载 Actions
2. **Nexus 私服**: `http://nexus.sephy.top` - 用于部署 JAR 包
3. **Maven Central**: 用于下载依赖（如果缓存未命中）
4. **飞书 Webhook**: 用于发送通知（如果启用）

### GitHub 托管 Runner

- 可以访问公网所有资源
- 如果 Nexus 在内网，需要使用自托管 Runner

### 自托管 Runner

- 根据网络环境配置代理
- 确保可以访问 Nexus 私服

## 工作流执行顺序

```
notify-start (通知开始)
    ↓
maven-deploy (Maven 部署)
    ├─→ Cache Maven dependencies (缓存恢复)
    ├─→ 配置 Maven settings (可选)
    ├─→ 部署 framework-dependencies
    └─→ 部署 framework-infra
    ↓
notify-success (成功通知) 或 notify-failure (失败通知)
```

## 故障排查

### 1. 部署失败：401 Unauthorized

**原因**: Maven 认证失败

**解决方法**:
- 检查 GitHub Secrets 中的 `NEXUS_USERNAME` 和 `NEXUS_PASSWORD` 是否正确
- 确认 Nexus 用户权限是否允许部署
- 查看工作流日志中的详细错误信息

### 2. 部署失败：Cannot connect to Nexus

**原因**: 网络不通

**解决方法**:
- 如果 Nexus 在内网，必须使用自托管 Runner
- 检查 Nexus URL 是否正确：`http://nexus.sephy.top`
- 使用自托管 Runner 时，测试网络连通性：
  ```bash
  curl http://nexus.sephy.top
  ```

### 3. 缓存未生效

**原因**: 缓存配置问题

**解决方法**:
- 检查 `pom.xml` 是否有变化（会生成新的缓存键）
- 查看工作流日志中的缓存命中情况
- GitHub Actions 缓存有 10GB 限制，过期缓存会自动清理

### 4. 通知未发送

**原因**: Webhook URL 未配置或配置错误

**解决方法**:
- 检查是否配置了 `FEISHU_WEBHOOK_URL` Secret
- 如果启用了签名校验，检查 `FEISHU_WEBHOOK_SECRET` 是否正确配置
- 测试飞书机器人 Webhook 是否可用：
  ```bash
  # 无签名校验的测试
  curl -X POST 'YOUR_WEBHOOK_URL' \
    -H 'Content-Type: application/json' \
    -d '{"msg_type":"text","content":{"text":"测试消息"}}'
  
  # 有签名校验的测试
  TIMESTAMP=$(date +%s)
  SECRET="your_secret_key"
  SIGN=$(echo -n -e "${TIMESTAMP}\n${SECRET}" | openssl dgst -sha256 -binary | base64)
  
  curl -X POST 'YOUR_WEBHOOK_URL' \
    -H 'Content-Type: application/json' \
    -d "{\"timestamp\":\"${TIMESTAMP}\",\"sign\":\"${SIGN}\",\"msg_type\":\"text\",\"content\":{\"text\":\"测试消息\"}}"
  ```
- 确认 self-hosted runner 可以访问飞书 API（检查网络和防火墙）
- 查看工作流日志中的 curl 命令输出
- 通知失败不影响主流程（使用 `if: always()` 确保总是执行）

**签名校验错误**:
- 如果收到 "sign invalid" 错误，检查：
  - `FEISHU_WEBHOOK_SECRET` 是否与飞书机器人配置的密钥一致
  - 时间戳格式是否正确（秒级时间戳）
  - 签名算法是否正确：`Base64(SHA256(timestamp + "\n" + secret))`

### 5. Docker 容器权限问题

**原因**: 容器内用户权限不足

**解决方法**:
- Workflow 已配置 `options: --user root` 确保权限
- 如果仍有问题，检查工作流日志中的详细错误

## 扩展配置

### 添加 Tag 推送触发

如果需要在推送 tag 时自动部署，可以修改 `on` 部分：

```yaml
on:
  workflow_dispatch:
    inputs:
      skip_tests:
        description: '是否跳过测试'
        required: false
        default: 'true'
        type: choice
        options:
          - 'true'
          - 'false'
  push:
    tags:
      - 'v*'
```

### 添加主分支推送触发

如果需要在推送到主分支时自动部署：

```yaml
on:
  workflow_dispatch:
    # ...
  push:
    branches:
      - main
      - master
```

### 添加构建矩阵（多版本测试）

如果需要在多个 Java 版本上测试：

```yaml
maven-deploy:
  strategy:
    matrix:
      java: [21, 25]
  steps:
    # ... 使用 ${{ matrix.java }} 配置 Java 版本
```

### 添加部署到多个仓库

如果需要同时部署到 snapshot 和 release 仓库：

```yaml
- name: 部署到 snapshot
  run: mvn deploy -DaltDeploymentRepository=nexus-snapshot::default::http://nexus.sephy.top/repository/maven-snapshots/

- name: 部署到 release
  run: mvn deploy -DaltDeploymentRepository=nexus-release::default::http://nexus.sephy.top/repository/maven-releases/
```

## 与 Woodpecker CI 的对比

| 特性 | Woodpecker CI | GitHub Actions |
|------|--------------|----------------|
| 配置文件 | `.woodpecker.yml` | `.github/workflows/*.yml` |
| 容器挂载 | ✅ 支持 | ❌ 不支持（托管 Runner）|
| 缓存策略 | 宿主机挂载 | `actions/cache` |
| Secrets | `from_secret` | `${{ secrets.* }}` |
| 触发方式 | push, pull_request | push, pull_request, workflow_dispatch |
| 通知集成 | 自定义脚本 | 自定义脚本（相同）|
| 网络访问 | 取决于部署环境 | 公网（托管）或配置（自托管）|

## 版本信息

- **Maven 版本**: 3.9.12
- **Java 版本**: Temurin 25
- **基础镜像**: `maven:3.9.12-eclipse-temurin-25-noble`

版本选择与项目的 `.mise.toml` 和 `.woodpecker.yml` 保持一致。

## 安全最佳实践

1. **Secrets 管理**：
   - 不要在代码中硬编码密码
   - 使用 GitHub Secrets 存储敏感信息
   - 定期轮换 Nexus 密码

2. **最小权限原则**：
   - Nexus 用户只授予部署所需的最小权限
   - 不要使用管理员账户进行部署

3. **审计日志**：
   - GitHub Actions 自动记录所有工作流执行
   - 可以在 Actions 页面查看历史记录

4. **分支保护**：
   - 配置分支保护规则
   - 要求部署前通过代码审查

## 成本考虑

### GitHub 托管 Runner

- 公共仓库：免费
- 私有仓库：根据 GitHub 定价计划
- 每月有免费额度（通常 2000-3000 分钟）

### 自托管 Runner

- 无 GitHub Actions 使用费用
- 需要自行维护服务器
- 适合内网部署场景

## 相关链接

- [GitHub Actions 官方文档](https://docs.github.com/en/actions)
- [actions/cache 使用指南](https://github.com/actions/cache)
- [Maven 部署最佳实践](https://maven.apache.org/guides/mini/guide-central-repository-upload.html)
