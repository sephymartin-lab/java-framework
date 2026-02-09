# DevOps Toolkit

通用的 DevOps 工具包仓库，提供项目常用的 CI/CD 脚本和代码格式化规范。

## 功能特性

- **CI/CD 脚本**: 可复用的 CI/CD 脚本，支持多种 CI/CD 平台（Woodpecker CI、GitHub Actions 等）
- **代码格式化规范**: 各语言的代码格式化配置文件（Java、Python、JavaScript、Go）

## 目录结构

```
devops-toolkit/
├── LICENSE              # MIT 许可证
├── README.md            # 项目文档
├── .gitignore           # Git 忽略规则
├── scripts/ci/          # CI/CD 脚本
│   ├── README.md        # CI 脚本使用指南
│   └── notify/          # 通知相关脚本
│       ├── feishu_notify.py      # 飞书通知核心模块
│       ├── notify_start.py        # 构建开始通知
│       └── notify_complete.py    # 构建完成通知
└── code-style/          # 代码格式化规范
    ├── java/            # Java 代码规范
    │   ├── p3c-eclipse-codestyle.xml    # 阿里巴巴 P3C Eclipse 代码风格
    │   └── eclipse.importorder          # Eclipse 导入顺序配置
    ├── python/          # Python 代码规范（计划中）
    ├── javascript/      # JavaScript/TypeScript 代码规范（计划中）
    └── golang/          # Go 代码规范（计划中）
```

## 快速开始

### 使用 Git Subtree 集成（推荐）

#### 1. 添加 subtree

```bash
# 添加整个 devops-toolkit 到项目
# 注意：请将 <repository-url> 替换为实际的仓库地址
git subtree add --prefix=scripts/devops-toolkit \
  <repository-url> main --squash
```

或者只添加特定目录：

```bash
# 只添加 CI 脚本
git subtree add --prefix=scripts/ci \
  <repository-url> main:scripts/ci --squash

# 只添加代码规范配置
git subtree add --prefix=code-style \
  <repository-url> main:code-style --squash
```

#### 2. 使用脚本

在 CI 配置文件中直接使用：

```yaml
# .woodpecker.yml
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
```

#### 3. 复制代码规范配置

```bash
# 复制 Java 代码规范配置（Eclipse/IntelliJ IDEA）
cp scripts/devops-toolkit/code-style/java/p3c-eclipse-codestyle.xml .idea/codeStyles/
cp scripts/devops-toolkit/code-style/java/eclipse.importorder .idea/
```

#### 4. 更新到最新版本

```bash
# 更新整个 devops-toolkit
# 注意：请将 <repository-url> 替换为实际的仓库地址
git subtree pull --prefix=scripts/devops-toolkit \
  <repository-url> main --squash
```

### 使用符号链接（本地开发）

如果多个项目在同一台机器上，可以使用符号链接：

```bash
# 创建符号链接
ln -s /path/to/devops-toolkit/scripts/ci ./scripts/ci-shared
```

## CI/CD 脚本

### 通知脚本

#### 飞书通知

支持在 CI 构建开始和完成时发送飞书通知。

**当前包含的脚本**：
- `feishu_notify.py`: 飞书 Webhook 通知核心模块，提供签名生成和消息发送功能
- `notify_start.py`: 在 CI 构建开始时发送飞书通知
- `notify_complete.py`: 在 CI 构建完成时发送飞书通知（支持成功/失败状态）

**环境变量要求**：
- `FEISHU_BOT_URL`: 飞书 Webhook URL（必需）
- `FEISHU_BOT_SECRET`: 飞书签名密钥（可选）

详细使用方法请参考 [scripts/ci/README.md](scripts/ci/README.md)。

### 扩展脚本

脚本目录支持扩展，可以添加：
- 构建脚本（Maven、Gradle、Docker 等）
- 测试脚本（单元测试、集成测试、覆盖率报告）
- 部署脚本（多环境部署）

欢迎贡献更多实用的 CI/CD 脚本！

## 代码格式化规范

代码格式化规范配置文件存放在 `code-style/` 目录下，按语言分类组织。

### Java

**当前包含的配置**：
- `p3c-eclipse-codestyle.xml`: 阿里巴巴 Java 开发手册（P3C）Eclipse 代码风格配置
- `eclipse.importorder`: Eclipse 导入顺序配置

**使用方法**：

1. **Eclipse/IntelliJ IDEA 导入代码风格**：
   - Eclipse: 导入 `p3c-eclipse-codestyle.xml` 作为代码格式化配置
   - IntelliJ IDEA: 可以导入 Eclipse 代码风格配置

2. **复制配置文件到项目**：
   ```bash
   cp code-style/java/p3c-eclipse-codestyle.xml .idea/codeStyles/
   cp code-style/java/eclipse.importorder .idea/
   ```

3. **使用格式化标签保护特殊代码**：
   
   如果某些代码需要保持特殊格式（如 SQL 语句、JSON 字符串等），可以使用格式化标签来保护：
   
   ```java
   // @formatter:off
   // 这段代码不会被格式化，保持原始格式
   String sql = "SELECT * FROM users WHERE id = ? AND name = ?";
   // @formatter:on
   
   // 正常代码会被格式化
   public void method() {
       // ...
   }
   ```
   
   **注意事项**：
   - 格式化标签已默认启用（`@formatter:off` 和 `@formatter:on`）
   - 仅在必要时使用，避免过度使用影响代码一致性
   - 适用于 SQL 语句、JSON 字符串、ASCII 图表等需要保持特定格式的场景

**计划添加的配置**：
- Checkstyle 配置
- Spotless 配置
- Google Java Format 配置

### Python

**计划支持的配置**：
- Black 配置
- isort 配置
- flake8 配置
- pylint 配置

### JavaScript/TypeScript

**计划支持的配置**：
- ESLint 配置
- Prettier 配置
- EditorConfig

### Go

**计划支持的配置**：
- golangci-lint 配置

> **提示**: 欢迎贡献其他语言的代码格式化配置！

## Git Subtree vs Submodule

| 特性 | Git Subtree | Git Submodule |
|------|-------------|---------------|
| 使用复杂度 | 简单，代码在主仓库中 | 需要额外命令初始化 |
| CI/CD 集成 | 无需特殊处理 | 需要初始化 submodule |
| 代码可见性 | 直接可见，像项目文件 | 需要进入 submodule 目录 |
| 更新方式 | `git subtree pull` | `git submodule update --remote` |
| 历史记录 | 合并到主项目历史 | 独立历史记录 |
| 适用场景 | 工具脚本、配置文件 | 需要独立版本管理的依赖 |

**推荐使用 Git Subtree**，因为：
- 使用更简单直观
- CI/CD 中无需特殊处理
- 团队成员无需学习额外命令

## 贡献指南

欢迎贡献代码和配置！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 贡献方向

- 添加新的 CI/CD 脚本（构建、测试、部署等）
- 完善代码格式化规范配置
- 改进文档和使用示例
- 修复 Bug 和优化现有脚本

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

Copyright (c) 2025 sephy.top

## 相关链接

- [CI/CD 脚本使用指南](scripts/ci/README.md)
