/*
 * Copyright 2022-2026 sephy.top
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.sephy.infra.playwright;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;

import lombok.extern.slf4j.Slf4j;

/**
 * Browser 池管理器
 * 管理不同浏览器类型的 Browser 对象池，支持 Browser 复用
 *
 * @author sephy
 * @date 2025-01-21
 */
@Slf4j
public class BrowserPoolManager implements InitializingBean, DisposableBean {

    private Playwright playwright;

    private final PlaywrightProperties properties;

    private final Map<String, GenericObjectPool<Browser>> browserPools = new ConcurrentHashMap<>();

    public BrowserPoolManager(PlaywrightProperties properties) {
        this.properties = properties;
    }

    /**
     * 设置 Playwright 实例（延迟初始化）
     * 因为 Playwright 实例来自池，所以需要在运行时设置
     */
    public void setPlaywright(Playwright playwright) {
        this.playwright = playwright;
    }

    /**
     * 从池中获取 Browser 实例
     *
     * @param browserType 浏览器类型（chromium/firefox/webkit）
     * @return Browser 实例
     * @throws Exception 获取失败时抛出异常
     */
    public Browser borrowBrowser(String browserType) throws Exception {
        ensureInitialized();
        String normalizedType = normalizeBrowserType(browserType);
        GenericObjectPool<Browser> pool = browserPools.get(normalizedType);
        if (pool == null) {
            // 如果池不存在，尝试初始化
            initializeBrowserPool(normalizedType);
            pool = browserPools.get(normalizedType);
            if (pool == null) {
                throw new IllegalArgumentException("Browser pool not initialized for type: " + browserType);
            }
        }
        return pool.borrowObject();
    }

    /**
     * 归还 Browser 实例到池中
     *
     * @param browserType 浏览器类型
     * @param browser Browser 实例
     */
    public void returnBrowser(String browserType, Browser browser) {
        if (browser == null) {
            return;
        }
        if (browserPools.isEmpty()) {
            // 如果池未初始化，直接关闭 Browser
            try {
                browser.close();
            } catch (Exception e) {
                log.error("Error closing browser when pool not initialized", e);
            }
            return;
        }
        String normalizedType = normalizeBrowserType(browserType);
        GenericObjectPool<Browser> pool = browserPools.get(normalizedType);
        if (pool != null) {
            try {
                pool.returnObject(browser);
            } catch (Exception e) {
                log.error("Error returning browser to pool", e);
                // 如果归还失败，尝试销毁该 Browser
                try {
                    browser.close();
                } catch (Exception ex) {
                    log.error("Error closing browser after return failure", ex);
                }
            }
        } else {
            // 如果对应的池不存在，直接关闭 Browser
            try {
                browser.close();
            } catch (Exception e) {
                log.error("Error closing browser when pool not found", e);
            }
        }
    }

    /**
     * 初始化 Browser 池
     * 注意：需要先设置 Playwright 实例
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (playwright == null) {
            log.warn("Playwright instance not set, BrowserPoolManager will be initialized on first use");
            return;
        }
        initializePools();
    }

    /**
     * 初始化所有需要的 Browser 池
     */
    private void initializePools() {
        if (playwright == null) {
            throw new IllegalStateException("Playwright instance must be set before initializing pools");
        }
        // 初始化默认浏览器类型的池（如果配置了）
        String defaultBrowserType = properties.getBrowderType();
        if (defaultBrowserType != null) {
            initializeBrowserPool(defaultBrowserType);
        } else {
            // 默认初始化 chromium 池
            initializeBrowserPool("chromium");
        }
        log.info("BrowserPoolManager initialized with {} pool(s)", browserPools.size());
    }

    /**
     * 确保 Browser 池已初始化（延迟初始化）
     */
    private void ensureInitialized() {
        if (playwright == null) {
            throw new IllegalStateException("Playwright instance must be set");
        }
        if (browserPools.isEmpty()) {
            initializePools();
        }
    }

    /**
     * 初始化指定浏览器类型的池
     *
     * @param browserType 浏览器类型
     */
    private void initializeBrowserPool(String browserType) {
        String normalizedType = normalizeBrowserType(browserType);
        if (browserPools.containsKey(normalizedType)) {
            log.debug("Browser pool already initialized for type: {}", normalizedType);
            return;
        }

        GenericObjectPoolConfig<Browser> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setJmxEnabled(false);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMaxTotal(properties.getMaxConcurrentInstance());
        poolConfig.setMaxIdle(properties.getMaxConcurrentInstance());
        poolConfig.setMinIdle(1);

        BrowserObjectFactory factory =
            new BrowserObjectFactory(playwright, normalizedType, properties.getLaunchOptions());

        GenericObjectPool<Browser> pool = new GenericObjectPool<>(factory, poolConfig);
        browserPools.put(normalizedType, pool);
        log.info("Initialized browser pool for type: {}, maxTotal: {}", normalizedType,
            properties.getMaxConcurrentInstance());
    }

    /**
     * 关闭所有 Browser 池
     */
    @Override
    public void destroy() throws Exception {
        for (Map.Entry<String, GenericObjectPool<Browser>> entry : browserPools.entrySet()) {
            try {
                entry.getValue().close();
                log.info("Closed browser pool for type: {}", entry.getKey());
            } catch (Exception e) {
                log.error("Error closing browser pool for type: {}", entry.getKey(), e);
            }
        }
        browserPools.clear();
    }

    /**
     * 规范化浏览器类型名称
     *
     * @param browserType 浏览器类型
     * @return 规范化后的浏览器类型
     */
    private String normalizeBrowserType(String browserType) {
        if (browserType == null || browserType.trim().isEmpty()) {
            return "chromium";
        }
        String normalized = browserType.trim().toLowerCase();
        if ("chromium".equals(normalized) || "firefox".equals(normalized) || "webkit".equals(normalized)) {
            return normalized;
        }
        // 默认返回 chromium
        log.warn("Unknown browser type: {}, using chromium as default", browserType);
        return "chromium";
    }

    /**
     * 获取池的统计信息（用于监控）
     *
     * @param browserType 浏览器类型
     * @return 池的统计信息
     */
    public PoolStats getPoolStats(String browserType) {
        String normalizedType = normalizeBrowserType(browserType);
        GenericObjectPool<Browser> pool = browserPools.get(normalizedType);
        if (pool == null) {
            return null;
        }
        return new PoolStats(pool.getNumActive(), pool.getNumIdle(), pool.getCreatedCount(), pool.getDestroyedCount());
    }

    /**
     * 池统计信息
     */
    public static class PoolStats {

        private final int numActive;

        private final int numIdle;

        private final long createdCount;

        private final long destroyedCount;

        public PoolStats(int numActive, int numIdle, long createdCount, long destroyedCount) {
            this.numActive = numActive;
            this.numIdle = numIdle;
            this.createdCount = createdCount;
            this.destroyedCount = destroyedCount;
        }

        public int getNumActive() {
            return numActive;
        }

        public int getNumIdle() {
            return numIdle;
        }

        public long getCreatedCount() {
            return createdCount;
        }

        public long getDestroyedCount() {
            return destroyedCount;
        }

        @Override
        public String toString() {
            return String.format("PoolStats{active=%d, idle=%d, created=%d, destroyed=%d}", numActive, numIdle,
                createdCount, destroyedCount);
        }
    }
}
