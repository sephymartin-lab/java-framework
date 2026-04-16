# Playwright 架构优化分析

## 当前架构问题

### 1. 资源浪费问题
- **现状**：每次调用都创建新的 Browser 实例
- **影响**：Browser 启动成本高（进程创建、初始化），频繁创建/销毁导致性能下降
- **数据**：Browser 启动通常需要 1-3 秒，而 BrowserContext 创建只需几毫秒

### 2. 会话隔离实现
- **正确做法**：通过创建新的 BrowserContext 实现会话隔离 ✅
- **问题**：但同时创建了新的 Browser（不必要）❌
- **优化**：可以复用 Browser，只创建新的 BrowserContext

### 3. 对象池验证问题
```java
// 当前实现：每次验证都发送网络请求
APIResponse apiResponse = p.getObject().request().newContext().get("https://www.baidu.com");
```
- **问题**：验证成本高，可能影响性能
- **建议**：使用更轻量的验证方式

## 优化方案

### 方案 1：Browser 实例复用（推荐）⭐⭐⭐

**核心思想**：复用 Browser 实例，每次只创建新的 BrowserContext

**优势**：
- ✅ 大幅减少 Browser 启动时间（从 1-3 秒降到几毫秒）
- ✅ 保持会话隔离（BrowserContext 隔离）
- ✅ 资源利用率更高

**实现要点**：
1. 创建 Browser 对象池（按浏览器类型分组）
2. 每次调用从池中获取 Browser
3. 创建新的 BrowserContext（确保会话隔离）
4. 使用完毕后关闭 Context，Browser 归还到池中

**架构图**：
```
Playwright (复用)
  └── Browser Pool (复用)
      ├── Browser 1
      │   ├── Context 1 (会话隔离) ✅
      │   ├── Context 2 (会话隔离) ✅
      │   └── Context 3 (会话隔离) ✅
      └── Browser 2
          ├── Context 4 (会话隔离) ✅
          └── Context 5 (会话隔离) ✅
```

### 方案 2：改进对象池验证 ⭐⭐

**当前问题**：
```java
public boolean validateObject(final PooledObject<Playwright> p) {
    APIResponse apiResponse = p.getObject().request().newContext().get("https://www.baidu.com");
    return apiResponse.ok();
}
```

**优化方案**：
```java
public boolean validateObject(final PooledObject<Playwright> p) {
    try {
        // 轻量级验证：检查 Playwright 实例是否仍然有效
        Playwright playwright = p.getObject();
        // 尝试创建一个简单的 BrowserType 实例来验证
        BrowserType browserType = playwright.chromium();
        return browserType != null;
    } catch (Exception e) {
        return false;
    }
}
```

**或者**：使用 `testWhileIdle` 而不是 `testOnBorrow`，减少验证频率

### 方案 3：支持动态配置 ⭐⭐

**当前问题**：所有调用使用同一个 `PlaywrightProperties`

**优化方案**：
```java
public <E> E doWithPlaywright(
    DefaultPlaywrightWorker<E> worker, 
    PlaywrightProperties overrideProperties  // 允许覆盖配置
) {
    // 合并默认配置和覆盖配置
    PlaywrightProperties mergedProperties = mergeProperties(
        playwrightProperties, 
        overrideProperties
    );
    // ...
}
```

### 方案 4：改进错误处理和监控 ⭐

**当前问题**：异常时只截图，缺少上下文信息

**优化方案**：
```java
catch (Exception e) {
    // 记录更多上下文信息
    log.error("Playwright execution failed", e);
    log.error("Browser type: {}, Context options: {}", browserType, contextOptions);
    
    // 截图 + 保存页面 HTML
    if (page != null) {
        saveScreenshot(page, properties);
        savePageHtml(page, properties);  // 新增
    }
    throw new RuntimeException(e);
}
```

## 性能对比

| 方案 | Browser 启动时间 | 会话隔离 | 资源利用率 | 实现复杂度 |
|------|-----------------|---------|-----------|-----------|
| **当前方案** | 每次 1-3 秒 | ✅ | ⭐⭐ | ⭐⭐ |
| **方案1（Browser复用）** | 首次 1-3 秒，后续几毫秒 | ✅ | ⭐⭐⭐ | ⭐⭐⭐ |
| **方案2（改进验证）** | 不变 | ✅ | ⭐⭐ | ⭐ |
| **方案3（动态配置）** | 不变 | ✅ | ⭐⭐ | ⭐⭐ |

## 推荐实施顺序

1. **第一步**：实施方案 1（Browser 复用）- 性能提升最明显
2. **第二步**：实施方案 2（改进验证）- 减少不必要的网络请求
3. **第三步**：实施方案 3（动态配置）- 提高灵活性
4. **第四步**：实施方案 4（改进错误处理）- 提升可观测性

## 注意事项

### Browser 复用的注意事项

1. **Browser 生命周期管理**
   - Browser 需要定期重启（避免内存泄漏）
   - 设置最大使用次数或最大存活时间

2. **资源清理**
   - 确保 BrowserContext 完全关闭
   - 监控 Browser 的内存使用情况

3. **并发控制**
   - Browser 可以同时创建多个 Context
   - 但需要控制每个 Browser 的最大 Context 数量

4. **浏览器类型隔离**
   - 不同浏览器类型（chromium/firefox/webkit）需要分别池化
   - 避免混用导致的配置冲突

## 代码示例

### Browser 复用实现示例

```java
public class BrowserPool {
    private final Map<String, GenericObjectPool<Browser>> browserPools;
    
    public Browser borrowBrowser(String browserType, BrowserType.LaunchOptions options) {
        GenericObjectPool<Browser> pool = browserPools.get(browserType);
        return pool.borrowObject();
    }
    
    public void returnBrowser(String browserType, Browser browser) {
        GenericObjectPool<Browser> pool = browserPools.get(browserType);
        pool.returnObject(browser);
    }
}
```

### 改进后的 DefaultPlaywrightWorker

```java
public T doWithPlaywright(Playwright playwright, PlaywrightProperties properties) {
    Browser browser = null;
    BrowserContext context = null;
    Page page = null;
    
    try {
        // 从 Browser 池获取（而不是每次都创建）
        browser = browserPool.borrowBrowser(
            properties.getBrowderType(), 
            properties.getLaunchOptions()
        );
        
        // 每次创建新的 Context（确保会话隔离）
        context = browser.newContext(properties.getContextOptions());
        
        // ... 后续逻辑不变
    } finally {
        // 关闭 Context（会话隔离的关键）
        if (context != null) context.close();
        
        // 归还 Browser 到池中（而不是关闭）
        if (browser != null) {
            browserPool.returnBrowser(properties.getBrowderType(), browser);
        }
    }
}
```
