# CI/CD 脚本使用指南

本目录包含可复用的 CI/CD 脚本，支持多种 CI/CD 平台。

## 目录结构

```
scripts/ci/
├── notify/          # 通知相关脚本
│   ├── feishu_notify.py      # 飞书通知核心模块
│   ├── notify_start.py        # 构建开始通知
│   └── notify_complete.py    # 构建完成通知
├── build/           # 构建相关脚本（可选）
├── test/            # 测试相关脚本（可选）
└── deploy/          # 部署相关脚本（可选）
```

## 通知脚本

### 飞书通知

#### 功能说明

- `feishu_notify.py`: 飞书 Webhook 通知核心模块，提供签名生成和消息发送功能
  - `send_message()`: 发送纯文本消息（向后兼容）
  - `send_post_message()`: 发送富文本 post 消息（支持加粗、链接等格式）
- `notify_start.py`: 在 CI 构建开始时发送飞书通知（使用富文本格式）
- `notify_complete.py`: 在 CI 构建完成时发送飞书通知（使用富文本格式，包含耗时统计）

#### 环境变量

- `FEISHU_BOT_URL`: 飞书 Webhook URL（必需）
- `FEISHU_BOT_SECRET`: 飞书签名密钥（可选）

#### CI 环境变量（自动获取）

通知脚本会自动从 CI 环境变量中获取以下信息：

- `CI_REPO_NAME`: 仓库名称
- `CI_PIPELINE_NUMBER`: 构建编号
- `CI_COMMIT_SHA`: 提交 SHA
- `CI_COMMIT_BRANCH`: 分支名称
- `CI_COMMIT_MESSAGE`: 提交信息
- `CI_PIPELINE_EVENT`: 构建事件类型
- `CI_PIPELINE_URL`: 构建详情 URL
- `CI_PIPELINE_STARTED`: 构建开始时间（UNIX 时间戳，用于计算耗时）

> **完整环境变量列表**: 请参考 [ENVIRONMENT_VARIABLES.md](./ENVIRONMENT_VARIABLES.md)

**耗时计算说明**：
- `notify_complete.py` 会自动计算构建耗时（从 `CI_PIPELINE_STARTED` 到当前时间）
- 如果 `CI_PIPELINE_STARTED` 不存在，耗时信息将不会显示
- 耗时格式：`2分35秒`、`45秒`、`1小时12分` 等

#### 使用方法

**Woodpecker CI 示例**:

```yaml
steps:
  - name: notify-start
    image: python:3-alpine
    environment:
      FEISHU_BOT_URL:
        from_secret: feishu_bot_url
      FEISHU_BOT_SECRET:
        from_secret: feishu_bot_secret
    commands:
      - python3 scripts/devops-toolkit/scripts/ci/notify/notify_start.py
    when:
      - event: [push, pull_request]
    failure: ignore

  - name: notify-success
    image: python:3-alpine
    environment:
      FEISHU_BOT_URL:
        from_secret: feishu_bot_url
      FEISHU_BOT_SECRET:
        from_secret: feishu_bot_secret
    commands:
      - python3 scripts/devops-toolkit/scripts/ci/notify/notify_complete.py success
    when:
      - status: success
    failure: ignore

  - name: notify-failure
    image: python:3-alpine
    environment:
      FEISHU_BOT_URL:
        from_secret: feishu_bot_url
      FEISHU_BOT_SECRET:
        from_secret: feishu_bot_secret
    commands:
      - python3 scripts/devops-toolkit/scripts/ci/notify/notify_complete.py failure
    when:
      - status: failure
    failure: ignore
```

**GitHub Actions 示例**:

```yaml
- name: Notify Start
  run: python3 scripts/devops-toolkit/scripts/ci/notify/notify_start.py
  env:
    FEISHU_BOT_URL: ${{ secrets.FEISHU_BOT_URL }}
    FEISHU_BOT_SECRET: ${{ secrets.FEISHU_BOT_SECRET }}
    CI_REPO_NAME: ${{ github.repository }}
    CI_PIPELINE_NUMBER: ${{ github.run_number }}
    CI_COMMIT_SHA: ${{ github.sha }}
    CI_COMMIT_BRANCH: ${{ github.ref_name }}
    CI_COMMIT_MESSAGE: ${{ github.event.head_commit.message }}
    CI_PIPELINE_EVENT: ${{ github.event_name }}
    CI_PIPELINE_URL: ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}
```

#### 注意事项

1. 确保脚本有执行权限：`chmod +x scripts/ci/notify/*.py`
2. Python 版本要求：Python 3.6+
3. 脚本依赖标准库，无需额外安装依赖
4. 敏感信息（URL 和密钥）通过环境变量传递，不要硬编码

## 使用 Git Subtree 集成

### 添加 subtree

```bash
# 添加整个 devops-toolkit
git subtree add --prefix=scripts/devops-toolkit \
  https://github.com/your-org/devops-toolkit.git main --squash

# 或者只添加 CI 脚本
git subtree add --prefix=scripts/ci \
  https://github.com/your-org/devops-toolkit.git main:scripts/ci --squash
```

### 更新 subtree

```bash
# 更新整个 devops-toolkit
git subtree pull --prefix=scripts/devops-toolkit \
  https://github.com/your-org/devops-toolkit.git main --squash

# 或者只更新 CI 脚本
git subtree pull --prefix=scripts/ci \
  https://github.com/your-org/devops-toolkit.git main:scripts/ci --squash
```

## 扩展脚本

如果需要添加新的脚本，请遵循以下规范：

1. 脚本独立可执行，不依赖特定项目结构
2. 通过环境变量配置，避免硬编码
3. 提供清晰的错误处理和日志输出
4. 在脚本开头添加使用说明注释
