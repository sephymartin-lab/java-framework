# Spotless 代码格式化校验 CI 集成文档

## 项目背景

本项目实现了在 Woodpecker CI 中对 Java 代码进行增量 Spotless 格式化校验的功能。当代码格式化不符合规范时，通过飞书机器人发送通知，并将失败信息记录到 DevOps 平台，支持统计哪些开发人员不遵守代码格式化规范。

## 核心需求

1. **触发时机**：Gitea 收到 push 请求后触发 CI
2. **校验范围**：只校验增量变更的 Java 文件（使用 spotless）
3. **连续提交场景**：如果服务器分支版本是 c1，客户端连续提交了 c2、c3，需要校验 c2 和 c3 的变更
4. **失败通知**：校验不通过时通过飞书机器人发送提示
5. **统计功能**：记录格式化失败信息到 DevOps 平台，统计哪些开发人员不遵守代码格式化规范

## 技术方案

### 架构设计

```
┌─────────────────┐
│  Gitea Push     │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  Woodpecker CI Workflow             │
│                                     │
│  ┌──────────────────────────────┐  │
│  │ Step 1: spotless-check       │  │
│  │ (Maven 镜像)                  │  │
│  │ - 清理旧文件                  │  │
│  │ - 检查所有模块                │  │
│  │ - 写入失败信息到文件          │  │
│  └──────────────┬───────────────┘  │
│                 │                   │
│                 ▼                   │
│  ┌──────────────────────────────┐  │
│  │ Step 2: spotless-notify      │  │
│  │ (Python 镜像)                │  │
│  │ - 读取失败信息                │  │
│  │ - 发送飞书通知                │  │
│  │ - 记录到 DevOps API           │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────┐     ┌──────────────┐
│   飞书机器人     │     │ DevOps 平台 │
│   (通知)        │     │   (统计)    │
└─────────────────┘     └──────────────┘
```

### 关键技术点

1. **增量文件检测**：使用 `git diff` 获取变更文件，只检查变更的 Java 文件
2. **Spotless 参数**：使用 `-DspotlessFiles` 参数动态指定文件列表（参考 `lint-staged.config.js`）
3. **上下文传递**：通过文件系统在步骤间传递失败信息（`.spotless_failed.flag` 和 `.spotless_failure.json`）
4. **HTTP API 集成**：通过 REST API 将失败信息记录到 DevOps 平台

## 文件结构

```
scripts/ci/spotless/
├── check_changed_files.sh          # Shell 版本：增量文件校验脚本
├── check_changed_files.py          # Python 版本（已废弃）
├── cleanup_failure_files.sh        # 清理旧的失败标志文件
├── check_all_modules.sh            # 检查所有模块
├── notify_if_failed.sh             # 检查失败并发送通知
├── notify_spotless_failure.py      # 格式化失败通知脚本
├── record_failure_api.py           # 通过 HTTP API 记录失败信息（推荐）
├── generate_statistics.py          # 统计报告生成脚本
└── database/                       # 数据库相关（已废弃，仅向后兼容）
    ├── config.py
    └── init_db.py
```

## CI 配置

### `.woodpecker.yml` 配置

```yaml
- name: spotless-check
  image: maven:3.9.12-eclipse-temurin-25-noble
  commands:
    - bash scripts/ci/spotless/cleanup_failure_files.sh
    - bash scripts/ci/spotless/check_all_modules.sh
  when:
    - event: [push]
  failure: ignore

- name: spotless-notify
  image: python:3-alpine
  environment:
    FEISHU_BOT_URL:
      from_secret: feishu_bot_url
    FEISHU_BOT_SECRET:
      from_secret: feishu_bot_secret
    DEVOPS_API_URL:
      from_secret: devops_api_url
    DEVOPS_API_TOKEN:
      from_secret: devops_api_token
  commands:
    - bash scripts/ci/spotless/notify_if_failed.sh
  when:
    - event: [push]
    - status: [failure, success]
  failure: ignore
```

## 环境变量

### CI 环境变量（自动获取）

| 变量名 | 说明 | 来源 |
|--------|------|------|
| `CI_COMMIT_SHA` | 当前提交 SHA | Woodpecker CI |
| `CI_PREV_COMMIT_SHA` | 上一个提交 SHA | Woodpecker CI |
| `CI_COMMIT_BRANCH` | 分支名称 | Woodpecker CI |
| `CI_COMMIT_MESSAGE` | 提交信息 | Woodpecker CI |
| `CI_COMMIT_AUTHOR` | 提交作者用户名 | Gitea（通过 Woodpecker CI） |
| `CI_COMMIT_AUTHOR_EMAIL` | 提交作者邮箱 | Gitea（通过 Woodpecker CI） |
| `CI_REPO_NAME` | 仓库名称 | Woodpecker CI |
| `CI_REPO_OWNER` | 仓库所有者 | Woodpecker CI |
| `CI_REPO` | 仓库全名（owner/name） | Woodpecker CI |
| `CI_PIPELINE_NUMBER` | 流水线编号 | Woodpecker CI |
| `CI_PIPELINE_URL` | 流水线 URL | Woodpecker CI |

### 配置环境变量

| 变量名 | 说明 | 必需 | 来源 |
|--------|------|------|------|
| `FEISHU_BOT_URL` | 飞书 Webhook URL | 是 | Secret |
| `FEISHU_BOT_SECRET` | 飞书签名密钥 | 否 | Secret |
| `DEVOPS_API_URL` | DevOps 平台 API 地址 | 是（如果使用 API） | Secret |
| `DEVOPS_API_TOKEN` | API 认证 Token | 是（如果使用 API） | Secret |

## API 接口规范

### 记录失败信息 API

**端点**：`POST {DEVOPS_API_URL}/api/v1/spotless/failures`

**认证**：Bearer Token（通过 `Authorization: Bearer {token}` header）

**请求头**：
```
Content-Type: application/json
Authorization: Bearer {DEVOPS_API_TOKEN}
```

**请求体**：
```json
{
  "commit_sha": "abc123def456...",
  "commit_branch": "main",
  "commit_message": "fix: update code formatting",
  "author_name": "张三",
  "author_email": "zhangsan@example.com",
  "failed_files": [
    "framework-dependencies/src/main/java/com/example/Test.java",
    "framework-infra/src/main/java/com/example/Service.java"
  ],
  "repo": "owner/repo-name",
  "repo_name": "repo-name",
  "repo_owner": "owner",
  "pipeline_number": "123",
  "pipeline_url": "https://ci.example.com/repos/7/pipeline/123",
  "timestamp": "2025-02-11T10:30:00"
}
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `commit_sha` | string | 提交 SHA（完整） |
| `commit_branch` | string | 分支名称 |
| `commit_message` | string | 提交信息 |
| `author_name` | string | 提交作者用户名（来自 Gitea） |
| `author_email` | string | 提交作者邮箱（来自 Gitea） |
| `failed_files` | array[string] | 失败文件列表（相对于项目根目录的完整路径） |
| `repo` | string | 仓库完整名称（owner/repo-name），用于唯一标识项目 |
| `repo_name` | string | 仓库名称（可选） |
| `repo_owner` | string | 仓库所有者（可选） |
| `pipeline_number` | string | 流水线编号 |
| `pipeline_url` | string | 流水线详情 URL |
| `timestamp` | string | ISO 8601 格式的时间戳 |

**响应**：
- 成功：`200 OK` 或 `201 Created`
- 失败：`400 Bad Request`（参数错误）或 `401 Unauthorized`（认证失败）

## 工作流程

### 1. spotless-check 步骤

1. **清理旧文件**：删除之前的失败标志文件和 JSON 文件
2. **检查模块**：依次检查 `framework-dependencies` 和 `framework-infra` 模块
3. **获取变更文件**：
   - 使用 `git diff --name-only --diff-filter=ACMR ${CI_PREV_COMMIT_SHA}..${CI_COMMIT_SHA}` 获取变更文件
   - 过滤出 `.java` 文件
   - 过滤出对应模块的文件
4. **运行 Spotless Check**：
   - 使用 `mvn spotless:check -DspotlessFiles=file1,file2,file3`
   - 文件路径是相对于项目根目录的完整路径（包含模块前缀）
5. **记录失败信息**：
   - 如果失败，在模块目录下创建 `.spotless_failure.json`
   - 在项目根目录创建 `.spotless_failed.flag` 标志文件

### 2. spotless-notify 步骤

1. **检查失败标志**：检查 `.spotless_failed.flag` 文件是否存在
2. **读取失败信息**：
   - 读取所有模块的 `.spotless_failure.json` 文件
   - 合并失败文件列表
3. **发送飞书通知**：
   - 构建富文本消息
   - 包含开发者信息、分支、提交、失败文件列表等
   - 调用飞书 API 发送通知
4. **记录到 DevOps 平台**：
   - 调用 `record_failure_api.py`
   - 通过 HTTP API 将失败信息发送到 DevOps 平台

## 关键实现细节

### 文件路径格式

- `git diff` 返回的路径：相对于项目根目录的完整路径
  - 例如：`framework-dependencies/src/main/java/...`
  - 例如：`framework-infra/src/main/java/...`
- 传递给 `-DspotlessFiles` 的路径：**相对于项目根目录的完整路径**（包含模块前缀）
  - 即使执行了 `cd framework-dependencies`，路径仍然需要包含 `framework-dependencies/` 前缀
  - 示例：`mvn spotless:check -DspotlessFiles=framework-dependencies/src/main/java/Test.java`

### 上下文传递机制

在 Woodpecker CI 中，同一个 workflow 的所有步骤共享同一个 workspace（文件系统），通过文件传递数据：

1. **失败状态**：通过 `.spotless_failed.flag` 标志文件
2. **失败文件列表**：通过各模块目录下的 `.spotless_failure.json` 文件
3. **文件位置**：
   - 项目根目录：`.spotless_failed.flag`
   - 模块目录：`framework-dependencies/.spotless_failure.json`、`framework-infra/.spotless_failure.json`

### 用户信息获取

- **来源**：Gitea（通过 Woodpecker CI 环境变量）
- **变量**：`CI_COMMIT_AUTHOR` 和 `CI_COMMIT_AUTHOR_EMAIL`
- **说明**：这些信息来自 Gitea 平台上的用户信息，不是从 git commit message 中解析的
- **优势**：更准确、更安全，不易被伪造

## 边界情况处理

1. **首次提交**：如果没有 `CI_PREV_COMMIT_SHA`，自动跳过校验
2. **无变更文件**：如果没有变更的 Java 文件，跳过校验
3. **非 Java 文件变更**：自动过滤，只检查 Java 文件
4. **多模块支持**：分别检查每个模块，合并失败信息
5. **API 失败**：API 请求失败不影响 CI 流程，只记录警告
6. **文件读写失败**：文件操作失败不影响主流程

## 使用示例

### 手动运行

```bash
# 检查指定模块
bash scripts/ci/spotless/check_changed_files.sh framework-dependencies

# 检查所有模块
bash scripts/ci/spotless/check_all_modules.sh

# 发送失败通知（如果失败）
bash scripts/ci/spotless/notify_if_failed.sh
```

### API 调用示例

```python
# 通过 Python 脚本记录失败信息
python3 scripts/ci/spotless/record_failure_api.py
```

## 配置 Secrets

在 Woodpecker CI 中配置以下 secrets：

1. `feishu_bot_url` - 飞书 Webhook URL
2. `feishu_bot_secret` - 飞书签名密钥（可选）
3. `devops_api_url` - DevOps 平台 API 地址（例如：`https://devops.example.com`）
4. `devops_api_token` - API 认证 Token

## 注意事项

1. **Spotless 配置**：确保项目已正确配置 `spotless-maven-plugin`
2. **文件路径**：传递给 `-DspotlessFiles` 的路径必须包含模块前缀
3. **Python 依赖**：`spotless-check` 步骤不需要 Python，`spotless-notify` 步骤需要 Python
4. **API 实现**：DevOps 平台需要实现对应的 API 端点
5. **幂等性**：建议 API 端点根据 `commit_sha` 实现幂等性，避免重复记录
6. **项目区分**：API 请求中包含 `repo` 字段，用于区分不同项目的失败记录

## 相关文件

- CI 配置：`.woodpecker.yml`
- Spotless 配置：`framework-dependencies/pom.xml`、`framework-infra/pom.xml`
- 参考实现：`lint-staged.config.js`（展示了如何使用 `-DspotlessFiles` 参数）
- 脚本目录：`scripts/ci/spotless/`

## 设计决策

### 为什么选择 HTTP API 而不是直接数据库？

1. **安全性**：不在 CI 中暴露数据库连接信息
2. **解耦**：CI 脚本不依赖数据库结构
3. **统一管理**：DevOps 平台统一处理数据存储、统计、报表
4. **标准化**：REST API 更易于集成和维护
5. **扩展性**：平台可以提供更多功能（实时统计、告警等）

### 为什么拆分两个步骤？

1. **职责分离**：Maven 步骤只做 check，Python 步骤只做通知
2. **镜像优化**：不需要在 Maven 镜像中安装 Python
3. **更清晰**：每个步骤的职责更明确
4. **可维护性**：更容易调试和维护

### 为什么使用文件传递上下文？

1. **简单可靠**：文件系统是 Woodpecker CI 步骤间共享的
2. **灵活性**：可以传递复杂的数据结构（JSON）
3. **可调试**：文件内容可以直接查看，便于调试

## 后续优化建议

1. **性能优化**：可以考虑并行检查多个模块
2. **缓存机制**：对于未变更的文件，可以跳过检查
3. **统计功能**：在 DevOps 平台实现统计查询 API，替代本地数据库
4. **告警规则**：在 DevOps 平台实现告警规则，如连续失败、失败率过高等
5. **批量处理**：对于大量失败文件，可以考虑批量发送通知

## 更新历史

- 2025-02-11：初始实现
  - 实现增量文件校验
  - 实现飞书通知
  - 实现 HTTP API 记录失败信息
  - 支持多模块检查
  - 支持项目名字区分
  - 拆分 CI 步骤，使用文件传递上下文
