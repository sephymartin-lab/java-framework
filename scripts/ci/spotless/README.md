# Spotless 代码格式化校验

本目录包含 Spotless 代码格式化校验相关的 CI 脚本。

## 功能说明

1. **增量文件校验**：只检查变更的 Java 文件，提高 CI 效率
2. **失败通知**：格式化校验失败时通过飞书机器人发送通知
3. **统计功能**：记录格式化失败信息到 DevOps 平台，支持统计查询

## 文件说明

### Shell 脚本
- `check_changed_files.sh` - 增量文件校验脚本（Shell 版本）
- `cleanup_failure_files.sh` - 清理旧的失败标志文件
- `check_all_modules.sh` - 检查所有模块
- `notify_if_failed.sh` - 检查失败并发送通知
- `init_database.sh` - 初始化数据库（仅在使用数据库方式时）

### Python 脚本
- `check_changed_files.py` - 增量文件校验脚本（Python 版本，已废弃）
- `notify_spotless_failure.py` - 格式化失败通知脚本
- `record_failure_api.py` - 通过 HTTP API 记录失败信息（推荐）
- `record_failure.py` - 直接数据库记录脚本（向后兼容）
- `generate_statistics.py` - 统计报告生成脚本（需要从 DevOps 平台获取数据）

### 数据库相关（仅在使用数据库方式时）
- `database/config.py` - 数据库配置模块
- `database/init_db.py` - 数据库初始化脚本

## 使用方法

### CI 集成

已在 `.woodpecker.yml` 中集成，会在 push 事件时自动执行。

### 手动运行

```bash
# 检查指定模块的变更文件
bash scripts/ci/spotless/check_changed_files.sh framework-dependencies

# 检查所有模块
bash scripts/ci/spotless/check_all_modules.sh

# 发送失败通知（如果失败）
bash scripts/ci/spotless/notify_if_failed.sh
```

## 配置说明

### HTTP API 方式（推荐）

通过 DevOps 平台的 HTTP API 记录失败信息：

```bash
export DEVOPS_API_URL=https://devops.example.com
export DEVOPS_API_TOKEN=your_api_token
```

**API 端点**：
- URL: `{DEVOPS_API_URL}/api/v1/spotless/failures`
- 方法: `POST`
- 认证: `Bearer Token`（通过 `Authorization` header）
- 请求体: JSON 格式

**请求体示例**：
```json
{
  "commit_sha": "abc123...",
  "commit_branch": "main",
  "commit_message": "fix: update code",
  "author_name": "张三",
  "author_email": "zhangsan@example.com",
  "failed_files": ["src/main/java/Test.java"],
  "repo": "owner/repo-name",
  "repo_name": "repo-name",
  "repo_owner": "owner",
  "pipeline_number": "123",
  "pipeline_url": "https://ci.example.com/pipeline/123",
  "timestamp": "2025-02-11T10:30:00"
}
```

**字段说明**：
- `repo`: 仓库完整名称（owner/repo-name），用于唯一标识项目
- `repo_name`: 仓库名称（可选）
- `repo_owner`: 仓库所有者（可选）

### 数据库方式（向后兼容）

如果未配置 `DEVOPS_API_URL`，会自动回退到数据库方式：

**SQLite（默认）**：
```bash
export SPOTLESS_DB_TYPE=sqlite
export SPOTLESS_DB_PATH=scripts/ci/spotless/database/spotless_failures.db
```

**MySQL（可选）**：
```bash
export SPOTLESS_DB_TYPE=mysql
export SPOTLESS_DB_HOST=localhost
export SPOTLESS_DB_PORT=3306
export SPOTLESS_DB_USER=spotless
export SPOTLESS_DB_PASSWORD=your_password
export SPOTLESS_DB_NAME=spotless_stats
```

## 环境变量

### CI 环境变量（自动获取）

- `CI_COMMIT_SHA` - 当前提交 SHA
- `CI_PREV_COMMIT_SHA` - 上一个提交 SHA
- `CI_COMMIT_BRANCH` - 分支名称
- `CI_COMMIT_MESSAGE` - 提交信息
- `CI_COMMIT_AUTHOR` - 提交作者
- `CI_COMMIT_AUTHOR_EMAIL` - 提交作者邮箱
- `CI_PIPELINE_NUMBER` - 流水线编号
- `CI_PIPELINE_URL` - 流水线 URL

### 飞书配置

- `FEISHU_BOT_URL` - 飞书 Webhook URL
- `FEISHU_BOT_SECRET` - 飞书签名密钥

### DevOps API 配置（推荐）

- `DEVOPS_API_URL` - DevOps 平台 API 地址
- `DEVOPS_API_TOKEN` - API 认证 Token

## 工作流程

1. **spotless-check 步骤**（Maven 镜像）：
   - 清理旧的失败标志文件
   - 检查所有模块的变更文件
   - 如果失败，创建标志文件和失败信息文件

2. **spotless-notify 步骤**（Python 镜像）：
   - 检查失败标志文件
   - 如果失败，发送飞书通知
   - 通过 HTTP API 或数据库记录失败信息

## 注意事项

1. **首次提交**：如果没有上一个提交，会自动跳过校验
2. **无变更文件**：如果没有变更的 Java 文件，会跳过校验
3. **数据库失败**：数据库连接失败不会影响 CI 流程，只会记录警告
4. **API 失败**：API 请求失败不会影响 CI 流程，只会记录警告
5. **多模块支持**：需要在每个 Maven 模块目录下分别运行检查

## API 实现建议

如果您的 DevOps 平台需要实现 API 端点，建议：

1. **端点路径**：`POST /api/v1/spotless/failures`
2. **认证方式**：Bearer Token
3. **请求格式**：JSON
4. **响应格式**：
   - 成功：`200 OK` 或 `201 Created`
   - 失败：`400 Bad Request`（参数错误）或 `401 Unauthorized`（认证失败）
5. **幂等性**：建议根据 `commit_sha` 实现幂等性，避免重复记录
