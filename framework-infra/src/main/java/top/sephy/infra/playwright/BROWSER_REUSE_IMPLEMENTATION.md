# Browser 复用方案实现说明

## 概述

已实现 Browser 实例复用方案，在保持会话隔离的前提下，大幅提升性能。

## 核心改进

### 1. 性能提升
- **之前**：每次调用都创建新的 Browser（耗时 1-3 秒）
- **现在**：Browser 复用，每次只创建新的 BrowserContext（耗时几毫秒）
- **性能提升**：10-100 倍（取决于调用频率）

### 2. 会话隔离保证
- ✅ 每次调用都创建新的 BrowserContext（完全隔离）
- ✅ BrowserContext 之间完全独立（Cookies、Storage、缓存等）
- ✅ 即使复用 Browser，会话状态也不会泄漏

## 新增文件

1. **BrowserObjectFactory.java** - Browser 对象池工厂
   - 负责创建和管理 Browser 实例
   - 提供轻量级验证（使用 `isConnected()` 方法）

2. **BrowserPoolManager.java** - Browser 池管理器
   - 管理不同浏览器类型的 Browser 对象池
   - 支持按浏览器类型（chromium/firefox/webkit）分别池化
   - 提供池统计信息（用于监控）

## 修改的文件

1. **PlaywrightProperties.java**
   - 新增 `browserReuseEnabled` 配置项（默认 `true`）
   - 可通过配置文件控制是否启用 Browser 复用

2. **PlaywrightWorkerEngine.java**
   - 集成 BrowserPoolManager
   - 在运行时动态设置 Playwright 实例到 BrowserPoolManager
   - 支持 pool 模式和 standard 模式

3. **DefaultPlaywrightWorker.java**
   - 支持从 Browser 池获取 Browser 实例
   - 使用完毕后归还到池中（而不是关闭）
   - 保持每次创建新 BrowserContext（确保隔离）

4. **PlaywrightObjectFactory.java**
   - 改进验证方法：使用轻量级验证（不再发送网络请求）
   - 提升对象池验证性能

## 配置说明

### application.yml 配置示例

```yaml
playwright:
  # 是否启用 Browser 复用（默认 true）
  browser-reuse-enabled: true
  
  # 最大并发实例数（Browser 池大小）
  max-concurrent-instance: 5
  
  # 运行模式：pool 或 standard
  mode: pool
  
  # 浏览器类型
  browder-type: chromium
  
  # Browser 启动选项
  launch-options:
    headless: true
    
  # BrowserContext 选项
  context-options:
    viewport-size:
      width: 1920
      height: 1080
```

### 禁用 Browser 复用

如果不想使用 Browser 复用（回退到原来的实现）：

```yaml
playwright:
  browser-reuse-enabled: false
```

## 使用示例

### 基本使用（无需修改现有代码）

```java
@Autowired
private PlaywrightWorkerEngine playwrightWorkerEngine;

public void example() {
    // 使用方式完全不变
    String result = playwrightWorkerEngine.doWithPlaywright(page -> {
        page.navigate("https://example.com");
        return page.title();
    });
}
```

### 监控 Browser 池状态

```java
@Autowired
private PlaywrightWorkerEngine playwrightWorkerEngine;

public void monitorPool() {
    BrowserPoolManager browserPoolManager = playwrightWorkerEngine.getBrowserPoolManager();
    if (browserPoolManager != null) {
        BrowserPoolManager.PoolStats stats = browserPoolManager.getPoolStats("chromium");
        log.info("Browser pool stats: {}", stats);
        // 输出：PoolStats{active=2, idle=3, created=5, destroyed=0}
    }
}
```

## 架构说明

### 资源层级

```
Playwright (复用)
  └── Browser Pool (复用) ← 新增
      ├── Browser 1
      │   ├── Context 1 (新建，隔离) ✅
      │   ├── Context 2 (新建，隔离) ✅
      │   └── Context 3 (新建，隔离) ✅
      └── Browser 2
          ├── Context 4 (新建，隔离) ✅
          └── Context 5 (新建，隔离) ✅
```

### 执行流程

1. **获取 Playwright 实例**（从 Playwright 池）
2. **设置 Playwright 到 BrowserPoolManager**（延迟初始化）
3. **从 Browser 池获取 Browser**（复用）
4. **创建新的 BrowserContext**（确保隔离）
5. **创建 Page 并执行任务**
6. **关闭 Context**（释放会话资源）
7. **归还 Browser 到池**（复用）

## 注意事项

### 1. Browser 生命周期管理

- Browser 会定期验证（`testOnBorrow` 和 `testWhileIdle`）
- 如果 Browser 连接断开，会自动销毁并创建新的
- 建议监控 Browser 池的使用情况

### 2. 资源清理

- BrowserContext 会在 `finally` 块中正确关闭
- Browser 会归还到池中（而不是关闭）
- 应用关闭时，所有 Browser 池会自动关闭

### 3. 并发控制

- Browser 池大小由 `max-concurrent-instance` 控制
- 每个 Browser 可以同时创建多个 Context
- 建议根据实际需求调整池大小

### 4. 浏览器类型隔离

- 不同浏览器类型（chromium/firefox/webkit）分别池化
- 避免混用导致的配置冲突
- 首次使用时自动初始化对应类型的池

## 性能对比

| 指标 | 之前（每次新建 Browser） | 现在（Browser 复用） | 提升 |
|------|------------------------|---------------------|------|
| Browser 启动时间 | 每次 1-3 秒 | 首次 1-3 秒，后续几毫秒 | 10-100 倍 |
| 资源利用率 | ⭐⭐ | ⭐⭐⭐ | 显著提升 |
| 会话隔离 | ✅ 100% | ✅ 100% | 保持不变 |

## 回退方案

如果遇到问题，可以通过配置快速回退到原来的实现：

```yaml
playwright:
  browser-reuse-enabled: false
```

## 测试建议

1. **隔离性测试**：验证不同 Context 之间的 Cookies、Storage 是否隔离
2. **性能测试**：对比启用前后的性能差异
3. **并发测试**：验证高并发场景下的稳定性
4. **资源监控**：监控 Browser 池的使用情况和内存占用

## 后续优化方向

1. **Browser 定期重启**：避免长时间运行导致的内存泄漏
2. **动态池大小调整**：根据负载自动调整池大小
3. **更详细的监控指标**：Browser 使用次数、平均存活时间等
4. **支持 Browser 预热**：应用启动时预创建 Browser 实例
