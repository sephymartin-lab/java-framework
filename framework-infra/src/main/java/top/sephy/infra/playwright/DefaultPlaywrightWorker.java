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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ScreenshotType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author sephy
 * @date 2022-02-11 22:09
 */
@Slf4j
public class DefaultPlaywrightWorker<T> {

    private PlaywrightPageWorker<T> playwrightPageWorker;

    public DefaultPlaywrightWorker(@NonNull PlaywrightPageWorker<T> playwrightPageWorker) {
        this.playwrightPageWorker = playwrightPageWorker;
    }

    public T doWithPlaywright(Playwright playwright) {
        return doWithPlaywright(playwright, null);
    }

    /**
     * 
     * @param playwright 测试
     * @param properties
     * @return
     */
    public T doWithPlaywright(Playwright playwright, PlaywrightProperties properties) {
        return doWithPlaywright(playwright, properties, null);
    }

    /**
     * 使用 Browser 池的版本
     *
     * @param playwright Playwright 实例
     * @param properties 配置属性
     * @param browserPoolManager Browser 池管理器（如果为 null，则每次创建新 Browser）
     * @return 执行结果
     */
    public T doWithPlaywright(Playwright playwright, PlaywrightProperties properties,
        BrowserPoolManager browserPoolManager) {
        BrowserType.LaunchOptions options = properties != null ? properties.getLaunchOptions() : null;
        Browser.NewContextOptions contextOptions = properties != null ? properties.getContextOptions() : null;

        String browserTypeName =
            properties != null && properties.getBrowderType() != null ? properties.getBrowderType() : "chromium";

        Browser browser = null;
        BrowserContext context = null;
        Page page = null;
        boolean browserFromPool = false;

        try {
            // 如果启用了 Browser 复用且有 BrowserPoolManager，从池中获取
            if (browserPoolManager != null && properties != null && properties.isBrowserReuseEnabled()) {
                try {
                    browser = browserPoolManager.borrowBrowser(browserTypeName);
                    browserFromPool = true;
                    log.debug("Borrowed browser from pool: {}", browserTypeName);
                } catch (Exception e) {
                    log.warn("Failed to borrow browser from pool, creating new one", e);
                    // 如果从池中获取失败，回退到创建新 Browser
                    browser = createNewBrowser(playwright, browserTypeName, options);
                }
            } else {
                // 未启用 Browser 复用，每次创建新 Browser
                browser = createNewBrowser(playwright, browserTypeName, options);
            }

            // 每次创建新的 BrowserContext（确保会话隔离）
            context = browser.newContext(contextOptions); //
            if (properties != null && !CollectionUtils.isEmpty(properties.getInitScripts())) {
                for (String path : properties.getInitScripts()) {
                    if (ResourcePatternUtils.isUrl(path)) {
                        try (InputStream inputStream = ResourceUtils.toURI(path).toURL().openStream()) {
                            context.addInitScript(StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8));
                        }
                    } else {
                        context.addInitScript(Paths.get(path));
                    }
                }
            }
            page = context.newPage();
            PlaywrightProperties.PageSettings pageSettings = properties.getPageSettings();
            if (pageSettings != null) {
                page.setDefaultTimeout(pageSettings.getDefaultTimeout());
            }
            return playwrightPageWorker.doWithPage(page, context);
        } catch (Exception e) {
            if (page != null) {
                String dir = properties != null && properties.getScreenshotDir() != null
                    ? properties.getScreenshotDir().toString() : "screenshot";
                LocalDateTime now = LocalDateTime.now();
                dir = dir + "/" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                try {
                    Files.createDirectories(Paths.get(dir));
                    page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get(dir, now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".jpg"))
                        .setType(ScreenshotType.JPEG));
                } catch (IOException ex) {
                    log.error("", ex);
                }
            }
            throw new RuntimeException(e);
        } finally {
            if (page != null) {
                try {
                    page.close();
                } catch (Exception e) {
                    log.error("close page error", e);
                }
            }
            if (context != null) {
                try {
                    context.close();
                } catch (Exception e) {
                    log.error("close context error", e);
                }
            }
            if (browser != null) {
                // 如果 Browser 来自池，归还到池中；否则关闭
                if (browserFromPool && browserPoolManager != null) {
                    browserPoolManager.returnBrowser(browserTypeName, browser);
                    log.debug("Returned browser to pool: {}", browserTypeName);
                } else {
                    try {
                        browser.close();
                    } catch (Exception e) {
                        log.error("browser close error", e);
                    }
                }
            }
        }
    }

    /**
     * 创建新的 Browser 实例
     *
     * @param playwright Playwright 实例
     * @param browserTypeName 浏览器类型名称
     * @param options 启动选项
     * @return Browser 实例
     */
    private Browser createNewBrowser(Playwright playwright, String browserTypeName, BrowserType.LaunchOptions options) {
        BrowserType browserType = playwright.chromium();
        if (browserTypeName != null) {
            if ("chromium".equalsIgnoreCase(browserTypeName)) {
                browserType = playwright.chromium();
            } else if ("firefox".equalsIgnoreCase(browserTypeName)) {
                browserType = playwright.firefox();
            } else if ("webkit".equalsIgnoreCase(browserTypeName)) {
                browserType = playwright.webkit();
            }
        }
        return browserType.launch(options);
    }
}
