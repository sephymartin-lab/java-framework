# Spotless CI 集成 - 快速上下文

## 核心功能

在 Woodpecker CI 中实现增量 Spotless 代码格式化校验，失败时发送飞书通知并记录到 DevOps 平台。

## 关键文件

- **CI 配置**：`.woodpecker.yml`（包含 `spotless-check` 和 `spotless-notify` 两个步骤）
- **校验脚本**：`scripts/ci/spotless/check_changed_files.sh`
- **通知脚本**：`scripts/ci/spotless/notify_spotless_failure.py`
- **API 记录**：`scripts/ci/spotless/record_failure_api.py`

## 工作流程

1. **spotless-check**（Maven 镜像）：检查变更文件，失败时写入 `.spotless_failed.flag` 和 `.spotless_failure.json`
2. **spotless-notify**（Python 镜像）：读取失败信息，发送飞书通知，通过 HTTP API 记录到 DevOps 平台

## 关键技术

- **增量检测**：`git diff ${CI_PREV_COMMIT_SHA}..${CI_COMMIT_SHA}`
- **Spotless 参数**：`mvn spotless:check -DspotlessFiles=file1,file2,file3`
- **文件路径**：相对于项目根目录的完整路径（包含模块前缀）
- **上下文传递**：通过文件系统（`.spotless_failed.flag` 和 `.spotless_failure.json`）

## API 端点

- **URL**：`POST {DEVOPS_API_URL}/api/v1/spotless/failures`
- **认证**：Bearer Token
- **关键字段**：`repo`（项目标识）、`author_name`、`author_email`、`failed_files`

## 环境变量

- `DEVOPS_API_URL`、`DEVOPS_API_TOKEN`（必需）
- `FEISHU_BOT_URL`、`FEISHU_BOT_SECRET`（通知用）
- CI 环境变量自动获取（`CI_COMMIT_SHA`、`CI_COMMIT_AUTHOR`、`CI_REPO` 等）

## 详细文档

完整文档请参考：`docs/spotless-ci-integration.md`
