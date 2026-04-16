# CI 环境变量参考文档

本文档列出了 Woodpecker CI 提供的内置环境变量，供开发 CI/CD 脚本时参考。

> **参考来源**: [Woodpecker CI 官方文档](https://woodpecker-ci.org/docs/usage/environment)  
> **版本**: 基于 Woodpecker CI 3.x

---

## Repository（仓库信息）

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `CI_REPO` | 仓库全名（owner/name） | `john-doe/my-repo` |
| `CI_REPO_OWNER` | 仓库所有者 | `john-doe` |
| `CI_REPO_NAME` | 仓库名称 | `my-repo` |
| `CI_REPO_REMOTE_ID` | 仓库在代码托管平台的 UID | `82` |
| `CI_REPO_URL` | 仓库 Web URL | `https://git.example.com/john-doe/my-repo` |
| `CI_REPO_CLONE_URL` | 仓库克隆 URL（HTTPS） | `https://git.example.com/john-doe/my-repo.git` |
| `CI_REPO_CLONE_SSH_URL` | 仓库克隆 URL（SSH） | `git@git.example.com:john-doe/my-repo.git` |
| `CI_REPO_DEFAULT_BRANCH` | 仓库默认分支 | `main` |
| `CI_REPO_PRIVATE` | 仓库是否为私有 | `true` / `false` |
| `CI_REPO_TRUSTED_NETWORK` | 仓库是否有可信网络访问权限 | `true` / `false` |
| `CI_REPO_TRUSTED_VOLUMES` | 仓库是否有可信卷访问权限 | `true` / `false` |
| `CI_REPO_TRUSTED_SECURITY` | 仓库是否有可信安全访问权限 | `true` / `false` |

---

## Current Commit（当前提交信息）

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `CI_COMMIT_SHA` | 提交 SHA | `eba09b46064473a1d345da7abf28b477468e8dbd` |
| `CI_COMMIT_REF` | 提交引用 | `refs/heads/main` |
| `CI_COMMIT_REFSPEC` | 提交引用规范 | `issue-branch:main` |
| `CI_COMMIT_BRANCH` | 提交分支（对于 PR 事件等于目标分支） | `main` |
| `CI_COMMIT_SOURCE_BRANCH` | 提交源分支（仅 PR 事件） | `issue-branch` |
| `CI_COMMIT_TARGET_BRANCH` | 提交目标分支（仅 PR 事件） | `main` |
| `CI_COMMIT_TAG` | 提交标签名（非 tag 事件时为空） | `v1.10.3` |
| `CI_COMMIT_PULL_REQUEST` | PR 编号（仅 PR 事件） | `1` |
| `CI_COMMIT_PULL_REQUEST_LABELS` | PR 标签（仅 PR 事件） | `server` |
| `CI_COMMIT_PULL_REQUEST_MILESTONE` | PR 里程碑（仅 `pull_request` 和 `pull_request_closed` 事件） | `summer-sprint` |
| `CI_COMMIT_MESSAGE` | 提交信息 | `Initial commit` |
| `CI_COMMIT_AUTHOR` | 提交作者用户名 | `john-doe` |
| `CI_COMMIT_AUTHOR_EMAIL` | 提交作者邮箱 | `john-doe@example.com` |
| `CI_COMMIT_PRERELEASE` | 是否为预发布版本（仅 release 事件） | `true` / `false` |

---

## Current Pipeline（当前流水线信息）

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `CI_PIPELINE_NUMBER` | 流水线编号 | `8` |
| `CI_PIPELINE_PARENT` | 父流水线编号 | `0` |
| `CI_PIPELINE_EVENT` | 流水线事件类型 | `push`, `pull_request`, `pull_request_closed`, `pull_request_metadata`, `tag`, `release`, `manual`, `cron` |
| `CI_PIPELINE_EVENT_REASON` | `pull_request_metadata` 事件的具体原因 | `label_updated`, `milestoned`, `demilestoned`, `assigned`, `edited`, ... |
| `CI_PIPELINE_URL` | 流水线 Web UI 链接 | `https://ci.example.com/repos/7/pipeline/8` |
| `CI_PIPELINE_FORGE_URL` | 代码托管平台的提交/标签链接 | `https://git.example.com/john-doe/my-repo/commit/eba09b46064473a1d345da7abf28b477468e8dbd` |
| `CI_PIPELINE_DEPLOY_TARGET` | 部署目标（仅 `deployment` 事件） | `production` |
| `CI_PIPELINE_DEPLOY_TASK` | 部署任务（仅 `deployment` 事件） | `migration` |
| `CI_PIPELINE_CREATED` | 流水线创建时间（UNIX 时间戳） | `1722617519` |
| `CI_PIPELINE_STARTED` | 流水线开始时间（UNIX 时间戳） | `1722617519` |
| `CI_PIPELINE_FILES` | 变更文件列表（仅 `push` 或 `pull_request` 事件，超过 500 个文件时未定义） | `[".woodpecker.yml","README.md"]` |
| `CI_PIPELINE_AUTHOR` | 流水线作者用户名 | `octocat` |
| `CI_PIPELINE_AVATAR` | 流水线作者头像 | `https://git.example.com/avatars/5dcbcadbce6f87f8abef` |

> **注意**: `CI_PIPELINE_FINISHED` 在 Woodpecker CI 3.0+ 中已移除（执行时恒为空）。如需计算耗时，请使用 `time.time() - int(CI_PIPELINE_STARTED)`。

---

## Current Workflow（当前工作流信息）

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `CI_WORKFLOW_NAME` | 工作流名称 | `release` |

---

## Current Step（当前步骤信息）

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `CI_STEP_NAME` | 步骤名称 | `build package` |
| `CI_STEP_NUMBER` | 步骤编号 | `0` |
| `CI_STEP_STARTED` | 步骤开始时间（UNIX 时间戳） | `1722617519` |
| `CI_STEP_URL` | 步骤 Web UI 链接 | `https://ci.example.com/repos/7/pipeline/8` |

> **注意**: `CI_STEP_FINISHED` 在 Woodpecker CI 3.0+ 中已移除（执行时恒为空）。

---

## Previous Commit（上一个提交信息）

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `CI_PREV_COMMIT_SHA` | 上一个提交 SHA | `15784117e4e103f36cba75a9e29da48046eb82c4` |
| `CI_PREV_COMMIT_REF` | 上一个提交引用 | `refs/heads/main` |
| `CI_PREV_COMMIT_REFSPEC` | 上一个提交引用规范 | `issue-branch:main` |
| `CI_PREV_COMMIT_BRANCH` | 上一个提交分支 | `main` |
| `CI_PREV_COMMIT_SOURCE_BRANCH` | 上一个提交源分支（仅 PR 事件） | `issue-branch` |
| `CI_PREV_COMMIT_TARGET_BRANCH` | 上一个提交目标分支（仅 PR 事件） | `main` |
| `CI_PREV_COMMIT_URL` | 上一个提交在代码托管平台的链接 | `https://git.example.com/john-doe/my-repo/commit/15784117e4e103f36cba75a9e29da48046eb82c4` |
| `CI_PREV_COMMIT_MESSAGE` | 上一个提交信息 | `test` |
| `CI_PREV_COMMIT_AUTHOR` | 上一个提交作者用户名 | `john-doe` |
| `CI_PREV_COMMIT_AUTHOR_EMAIL` | 上一个提交作者邮箱 | `john-doe@example.com` |

---

## Previous Pipeline（上一个流水线信息）

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `CI_PREV_PIPELINE_NUMBER` | 上一个流水线编号 | `7` |
| `CI_PREV_PIPELINE_PARENT` | 上一个父流水线编号 | `0` |
| `CI_PREV_PIPELINE_EVENT` | 上一个流水线事件类型 | `push`, `pull_request`, ... |
| `CI_PREV_PIPELINE_EVENT_REASON` | 上一个流水线事件原因 | `label_updated`, ... |
| `CI_PREV_PIPELINE_URL` | 上一个流水线 Web UI 链接 | `https://ci.example.com/repos/7/pipeline/7` |
| `CI_PREV_PIPELINE_FORGE_URL` | 上一个流水线在代码托管平台的链接 | `https://git.example.com/john-doe/my-repo/commit/15784117e4e103f36cba75a9e29da48046eb82c4` |
| `CI_PREV_PIPELINE_DEPLOY_TARGET` | 上一个流水线部署目标（仅 `deployment` 事件） | `production` |
| `CI_PREV_PIPELINE_DEPLOY_TASK` | 上一个流水线部署任务（仅 `deployment` 事件） | `migration` |
| `CI_PREV_PIPELINE_STATUS` | 上一个流水线状态 | `success`, `failure` |
| `CI_PREV_PIPELINE_CREATED` | 上一个流水线创建时间（UNIX 时间戳） | `1722610173` |
| `CI_PREV_PIPELINE_STARTED` | 上一个流水线开始时间（UNIX 时间戳） | `1722610173` |
| `CI_PREV_PIPELINE_FINISHED` | 上一个流水线完成时间（UNIX 时间戳） | `1722610383` |
| `CI_PREV_PIPELINE_AUTHOR` | 上一个流水线作者用户名 | `octocat` |
| `CI_PREV_PIPELINE_AVATAR` | 上一个流水线作者头像 | `https://git.example.com/avatars/5dcbcadbce6f87f8abef` |

---

## System（系统信息）

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `CI_SYSTEM_NAME` | CI 系统名称 | `woodpecker` |
| `CI_SYSTEM_URL` | CI 系统 URL | `https://ci.example.com` |
| `CI_SYSTEM_HOST` | CI 服务器主机名 | `ci.example.com` |
| `CI_SYSTEM_VERSION` | CI 服务器版本 | `2.7.0` |

---

## Forge（代码托管平台信息）

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `CI_FORGE_TYPE` | 代码托管平台类型 | `bitbucket`, `bitbucket_dc`, `forgejo`, `gitea`, `github`, `gitlab` |
| `CI_FORGE_URL` | 代码托管平台根 URL | `https://git.example.com` |

---

## Workspace（工作空间）

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `CI_WORKSPACE` | 源代码克隆到的工作空间路径 | `/woodpecker/src/git.example.com/john-doe/my-repo` |

---

## 版本变更说明

### Woodpecker CI 3.0+ 移除的变量

以下变量在 Woodpecker CI 3.0+ 中已移除（执行时恒为空）：

- `CI_PIPELINE_FINISHED` - 流水线完成时间
- `CI_STEP_FINISHED` - 步骤完成时间
- `CI_PIPELINE_STATUS` - 流水线状态（始终为 `success`）
- `CI_STEP_STATUS` - 步骤状态（始终为 `success`）

**替代方案**：
- 计算耗时：使用 `time.time() - int(CI_PIPELINE_STARTED)` 或 `time.time() - int(CI_STEP_STARTED)`
- 获取状态：通过命令行参数或条件判断（如 `when: status: success`）

---

## 使用示例

### 计算流水线耗时

```python
import os
import time

pipeline_started = os.environ.get('CI_PIPELINE_STARTED')
if pipeline_started:
    started_time = int(pipeline_started)
    current_time = int(time.time())
    duration_seconds = current_time - started_time
    print(f"流水线耗时: {duration_seconds} 秒")
```

### 获取提交信息

```python
import os

commit_sha = os.environ.get('CI_COMMIT_SHA', 'unknown')
commit_branch = os.environ.get('CI_COMMIT_BRANCH', 'unknown')
commit_message = os.environ.get('CI_COMMIT_MESSAGE', 'unknown')

print(f"提交: {commit_sha[:8]}")  # 显示前8位
print(f"分支: {commit_branch}")
print(f"信息: {commit_message.split(chr(10))[0]}")  # 只显示第一行
```

### 判断事件类型

```python
import os

event = os.environ.get('CI_PIPELINE_EVENT', 'unknown')
if event == 'pull_request':
    pr_number = os.environ.get('CI_COMMIT_PULL_REQUEST')
    print(f"PR #{pr_number}")
elif event == 'tag':
    tag = os.environ.get('CI_COMMIT_TAG')
    print(f"标签: {tag}")
```

---

## 参考链接

- [Woodpecker CI 官方文档 - 环境变量](https://woodpecker-ci.org/docs/usage/environment)
- [Woodpecker CI 迁移指南](https://woodpecker-ci.org/docs/migrations)
