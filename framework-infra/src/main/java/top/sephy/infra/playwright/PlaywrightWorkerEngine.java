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

import java.util.concurrent.Semaphore;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author sephy
 * @date 2022-02-11 22:06
 */
@Slf4j
public class PlaywrightWorkerEngine implements InitializingBean, DisposableBean {

    private GenericObjectPool<Playwright> playwrightObjectPool;

    private final PlaywrightProperties playwrightProperties;

    private Semaphore semaphore;

    private BrowserPoolManager browserPoolManager;

    private static final String MODE_POOL = "pool";

    private static final String MODE_STANDARD = "standard";

    public PlaywrightWorkerEngine(PlaywrightProperties playwrightProperties) {
        this.playwrightProperties = playwrightProperties;
    }

    public <E> E doWithPlaywright(PlaywrightPageWorker<E> worker) {
        return doWithPlaywright(new DefaultPlaywrightWorker<>(worker), playwrightProperties);
    }

    public <E> E doWithPlaywright(DefaultPlaywrightWorker<E> worker) {
        return doWithPlaywright(worker, playwrightProperties);
    }

    @SneakyThrows
    public <E> E doWithPlaywright(DefaultPlaywrightWorker<E> worker, PlaywrightProperties playwrightProperties) {

        if (MODE_POOL.equalsIgnoreCase(playwrightProperties.getMode())) {
            Playwright playwright = null;
            try {
                playwright = playwrightObjectPool.borrowObject();
                // 如果启用了 Browser 复用，设置 Playwright 实例到 BrowserPoolManager
                if (playwrightProperties.isBrowserReuseEnabled() && browserPoolManager != null) {
                    browserPoolManager.setPlaywright(playwright);
                    return worker.doWithPlaywright(playwright, playwrightProperties, browserPoolManager);
                } else {
                    return worker.doWithPlaywright(playwright, playwrightProperties);
                }
            } catch (Exception e) {
                throw new PlaywrightException("Execute failed.", e);
            } finally {
                if (playwright != null) {
                    playwrightObjectPool.returnObject(playwright);
                }
            }
        } else {
            semaphore.acquire();
            try (Playwright playwright = Playwright.create(playwrightProperties.getCreateOptions());) {
                // 如果启用了 Browser 复用，设置 Playwright 实例到 BrowserPoolManager
                if (playwrightProperties.isBrowserReuseEnabled() && browserPoolManager != null) {
                    browserPoolManager.setPlaywright(playwright);
                    return worker.doWithPlaywright(playwright, playwrightProperties, browserPoolManager);
                } else {
                    return worker.doWithPlaywright(playwright, playwrightProperties);
                }
            } finally {
                semaphore.release();
            }
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (MODE_POOL.equalsIgnoreCase(playwrightProperties.getMode())) {
            GenericObjectPoolConfig<Playwright> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setJmxEnabled(false);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setMaxTotal(playwrightProperties.getMaxConcurrentInstance());
            playwrightObjectPool = new GenericObjectPool<>(
                new PlaywrightObjectFactory(playwrightProperties.getCreateOptions()), poolConfig);

            // 如果启用了 Browser 复用，创建 BrowserPoolManager
            if (playwrightProperties.isBrowserReuseEnabled()) {
                browserPoolManager = new BrowserPoolManager(playwrightProperties);
                log.info("Browser reuse enabled, BrowserPoolManager created (will be initialized on first use)");
            }
        } else {
            semaphore = new Semaphore(playwrightProperties.getMaxConcurrentInstance());
            // standard 模式也可以使用 Browser 复用
            if (playwrightProperties.isBrowserReuseEnabled()) {
                browserPoolManager = new BrowserPoolManager(playwrightProperties);
                log.info("Browser reuse enabled for standard mode, BrowserPoolManager created");
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        if (MODE_POOL.equalsIgnoreCase(playwrightProperties.getMode())) {
            playwrightObjectPool.close();
        }
        if (browserPoolManager != null) {
            browserPoolManager.destroy();
        }
    }

    /**
     * 获取 BrowserPoolManager（用于监控）
     *
     * @return BrowserPoolManager 实例，如果未启用 Browser 复用则返回 null
     */
    public BrowserPoolManager getBrowserPoolManager() {
        return browserPoolManager;
    }
}
