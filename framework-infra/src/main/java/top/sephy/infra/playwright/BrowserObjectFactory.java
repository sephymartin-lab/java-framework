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

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import lombok.extern.slf4j.Slf4j;

/**
 * Browser 对象池工厂
 * 用于创建和管理 Browser 实例，支持 Browser 复用
 *
 * @author sephy
 * @date 2025-01-21
 */
@Slf4j
public class BrowserObjectFactory extends BasePooledObjectFactory<Browser> {

    private final Playwright playwright;

    private final String browserType;

    private final BrowserType.LaunchOptions launchOptions;

    public BrowserObjectFactory(Playwright playwright, String browserType, BrowserType.LaunchOptions launchOptions) {
        this.playwright = playwright;
        this.browserType = browserType;
        this.launchOptions = launchOptions;
    }

    @Override
    public Browser create() throws Exception {
        BrowserType browserTypeInstance = getBrowserType(browserType);
        if (browserTypeInstance == null) {
            throw new IllegalArgumentException("Unsupported browser type: " + browserType);
        }
        Browser browser = browserTypeInstance.launch(launchOptions);
        log.debug("Created new Browser instance: {}", browserType);
        return browser;
    }

    @Override
    public PooledObject<Browser> wrap(Browser browser) {
        return new DefaultPooledBrowser(browser);
    }

    @Override
    public void destroyObject(PooledObject<Browser> p) throws Exception {
        Browser browser = p.getObject();
        try {
            browser.close();
            log.debug("Destroyed Browser instance: {}", browserType);
        } catch (Exception e) {
            log.error("Error destroying Browser instance", e);
            throw e;
        }
    }

    @Override
    public boolean validateObject(final PooledObject<Browser> p) {
        try {
            Browser browser = p.getObject();
            // 轻量级验证：检查 Browser 是否仍然连接
            // Browser.isConnected() 方法可以检查浏览器进程是否仍然活跃
            return browser != null && browser.isConnected();
        } catch (Exception e) {
            log.warn("Browser validation failed", e);
            return false;
        }
    }

    private BrowserType getBrowserType(String browserType) {
        if (browserType == null || "chromium".equalsIgnoreCase(browserType)) {
            return playwright.chromium();
        } else if ("firefox".equalsIgnoreCase(browserType)) {
            return playwright.firefox();
        } else if ("webkit".equalsIgnoreCase(browserType)) {
            return playwright.webkit();
        }
        return null;
    }

    private static class DefaultPooledBrowser extends DefaultPooledObject<Browser> {

        public DefaultPooledBrowser(Browser browser) {
            super(browser);
        }
    }
}
