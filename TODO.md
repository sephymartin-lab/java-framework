# Java Framework 待办事项

## 模块重构方案

**状态**: 待执行  
**创建时间**: 2026-01-24  
**优先级**: 中

### 当前问题分析

#### 现状
- **framework-dependencies**: 依赖管理模块（BOM），职责清晰
- **framework-infra**: 单一大型模块，包含 149 个 Java 文件，涵盖所有基础设施功能

#### 存在的问题
1. **职责混乱**: 一个模块包含太多不同领域的功能（MyBatis、安全、Redis、Web、工具类等）
2. **依赖耦合**: 所有功能打包在一起，无法按需引入
3. **维护困难**: 模块过大，不利于独立演进和版本管理
4. **测试复杂**: 模块间边界不清晰，测试范围过大

### 推荐的模块划分方案

#### 方案一：按技术领域划分（推荐）⭐

```
java-framework/
├── framework-dependencies/          # 依赖管理（保持不变）
├── framework-common/                 # 通用基础模块
│   ├── framework-common-core/        # 核心基础类（异常、常量、实体基类）
│   └── framework-common-utils/      # 工具类（BigDecimalUtils、SpELUtils 等）
├── framework-data/                   # 数据访问层
│   ├── framework-data-mybatis/      # MyBatis 相关（type handlers、interceptors、query）
│   └── framework-data-mybatis-plus/ # MyBatis-Plus 扩展（injectors、interceptors）
├── framework-web/                    # Web 层
│   ├── framework-web-core/          # Web 核心（CommonResult、GlobalExceptionHandler）
│   ├── framework-web-jackson/       # Jackson 序列化（serializers、deserializers、annotations）
│   └── framework-web-security/      # 安全相关（auth、security、filter）
├── framework-cache/                  # 缓存层
│   └── framework-cache-redis/       # Redis 相关（lock、redis 工具）
├── framework-monitor/                # 监控层
│   ├── framework-monitor-logging/   # 日志（logging、annotation）
│   └── framework-monitor-micrometer/# 指标监控（micrometer）
├── framework-integration/            # 集成层
│   ├── framework-integration-playwright/ # Playwright 浏览器自动化
│   └── framework-integration-option/     # 字典/选项功能
└── framework-starter/                # Starter 模块（可选）
    ├── framework-starter-web/        # Web Starter（自动配置）
    └── framework-starter-data/       # Data Starter（自动配置）
```

#### 方案二：按层次划分

```
java-framework/
├── framework-dependencies/
├── framework-core/                   # 核心层（异常、常量、实体、工具）
├── framework-persistence/            # 持久化层（MyBatis、MyBatis-Plus）
├── framework-presentation/           # 表现层（Web、Jackson、Security）
├── framework-infrastructure/         # 基础设施层（Redis、Lock、Thread）
└── framework-integration/            # 集成层（Playwright、Option、Event）
```

#### 方案三：最小化拆分（保守方案）

```
java-framework/
├── framework-dependencies/
├── framework-core/                   # 核心基础（异常、常量、实体、工具、配置）
├── framework-data/                   # 数据访问（MyBatis、MyBatis-Plus）
├── framework-web/                    # Web 相关（CommonResult、Jackson、Security、Filter）
├── framework-cache/                  # 缓存（Redis、Lock）
└── framework-integration/            # 集成（Playwright、Option、Event、Command、Thread）
```

### 详细模块职责说明

#### framework-common-core
- `exception/` - 异常类（SystemException、ServiceException）
- `consts/` - 常量类（AopOrderConstants、HttpHeaderConstants）
- `entity/` - 实体基类
- `config/` - 通用配置

#### framework-common-utils
- `utils/` - 工具类（BigDecimalUtils、SpELUtils、JacksonUtils）

#### framework-data-mybatis
- `mybatis/type/` - Type Handlers
- `mybatis/interceptor/` - Interceptors
- `mybatis/query/` - 查询构建
- `mybatis/audit/` - 审计功能
- `mybatis/optimistic/` - 乐观锁

#### framework-data-mybatis-plus
- `mybatis/plus/injector/` - 自定义注入器
- `mybatis/plus/intercepter/` - 拦截器

#### framework-web-core
- `web/CommonResult` - 统一响应
- `web/GlobalExceptionHandler` - 全局异常处理
- `web/filter/` - Web 过滤器

#### framework-web-jackson
- `jackson/ser/` - 序列化器
- `jackson/deser/` - 反序列化器
- `jackson/annotation/` - Jackson 注解

#### framework-web-security
- `security/` - Spring Security 扩展
- `auth/` - 认证相关

#### framework-cache-redis
- `redis/` - Redis 工具
- `lock/` - 分布式锁

#### framework-monitor-logging
- `logging/annotation/` - 日志注解和 AOP

#### framework-integration-playwright
- `playwright/` - Playwright 浏览器自动化

#### framework-integration-option
- `option/` - 字典/选项功能

### 迁移策略

#### 阶段一：创建新模块结构
1. 创建新的模块目录和 pom.xml
2. 保持 framework-infra 模块不变（向后兼容）

#### 阶段二：代码迁移
1. 按包结构迁移代码到对应模块
2. 更新包名（可选，建议保持 `top.sephy.infra.*`）
3. 调整模块间依赖关系

#### 阶段三：更新依赖
1. 更新使用该框架的项目依赖
2. 从 `framework-infra` 迁移到新的模块依赖

#### 阶段四：清理
1. 标记 `framework-infra` 为废弃
2. 最终移除 `framework-infra` 模块

### 模块依赖关系

```
framework-dependencies (BOM)
    ↑
framework-common-core
    ↑
framework-common-utils (依赖 core)
framework-data-mybatis (依赖 core)
framework-web-core (依赖 core)
framework-cache-redis (依赖 core)
    ↑
framework-data-mybatis-plus (依赖 mybatis)
framework-web-jackson (依赖 core)
framework-web-security (依赖 core, web-core)
framework-monitor-logging (依赖 core)
    ↑
framework-integration-playwright (依赖 core, utils)
framework-integration-option (依赖 core, cache-redis)
```

### 优势

1. **职责清晰**: 每个模块职责单一，易于理解
2. **按需引入**: 项目可以只引入需要的模块
3. **独立演进**: 各模块可以独立版本管理和演进
4. **测试友好**: 模块边界清晰，测试范围明确
5. **依赖管理**: 减少不必要的依赖传递

### 建议

推荐采用**方案一（按技术领域划分）**，因为：
- 符合 Spring Boot Starter 的设计理念
- 模块职责清晰，易于维护
- 支持按需引入，减少依赖体积
- 便于后续扩展新的技术领域模块

### 执行任务清单

- [ ] 分析当前 framework-infra 模块的包结构和代码分布
- [ ] 设计新的模块划分方案和依赖关系
- [ ] 创建新模块的 pom.xml 文件，定义模块依赖关系
- [ ] 将代码从 framework-infra 迁移到对应的新模块
- [ ] 更新包导入路径（如需要）
- [ ] 创建迁移指南文档，说明如何从旧模块迁移到新模块
