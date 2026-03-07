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
package top.sephy.infra.config;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

/**
 * 配置文件监听器，自动检测 application.yml 变化并触发配置刷新
 * 
 * @author sephy
 */
@Slf4j
public class ConfigFileWatcher implements SmartLifecycle {

    private final ContextRefresher contextRefresher;
    private final Environment environment;
    private final ConfigFileWatcherProperties properties;
    private WatchService watchService;
    private Thread watchThread;
    private volatile boolean running = false;

    public ConfigFileWatcher(ContextRefresher contextRefresher, Environment environment,
        ConfigFileWatcherProperties properties) {
        this.contextRefresher = contextRefresher;
        this.environment = environment;
        this.properties = properties;
    }

    @Override
    public void start() {
        if (running) {
            log.warn("配置文件监听器已经在运行中");
            return;
        }

        if (!properties.isEnabled()) {
            log.info("配置文件监听功能未启用，跳过启动");
            return;
        }

        try {
            // 获取配置文件路径
            String configLocation = environment.getProperty("spring.config.location");
            String configName = environment.getProperty("spring.config.name", "application");
            String activeProfile = environment.getProperty("spring.profiles.active", "");

            // 尝试找到 application.yml 文件
            Path configFile = findConfigFile(configName, activeProfile);
            if (configFile == null) {
                log.warn("未找到配置文件，跳过文件监听");
                return;
            }

            Path configDir = configFile.getParent();
            log.info("开始监听配置文件: {}", configFile.toAbsolutePath());

            // 创建 WatchService
            watchService = FileSystems.getDefault().newWatchService();
            configDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            // 启动监听线程
            watchThread = new Thread(this::watchConfigFile, "config-file-watcher");
            watchThread.setDaemon(true);
            watchThread.start();

            running = true;
            log.info("配置文件监听器已启动，监听目录: {}", configDir.toAbsolutePath());
        } catch (Exception e) {
            log.error("启动配置文件监听器失败", e);
        }
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }

        log.info("正在停止配置文件监听器...");
        running = false;

        if (watchService != null) {
            try {
                watchService.close();
            } catch (Exception e) {
                log.error("关闭 WatchService 失败", e);
            }
        }

        if (watchThread != null && watchThread.isAlive()) {
            watchThread.interrupt();
            try {
                watchThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("配置文件监听器已停止");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return properties.isEnabled();
    }

    @Override
    public int getPhase() {
        // 在其他 SmartLifecycle 组件之后启动
        return 1;
    }

    /**
     * 查找配置文件
     */
    private Path findConfigFile(String configName, String activeProfile) {
        // 首先尝试从类路径查找（最常见的情况）
        try {
            java.net.URL resource = getClass().getClassLoader().getResource(configName + ".yml");
            if (resource != null && "file".equals(resource.getProtocol())) {
                Path path = Paths.get(resource.toURI());
                if (java.nio.file.Files.exists(path)) {
                    return path;
                }
            }
        } catch (Exception e) {
            log.debug("从类路径查找配置文件失败: {}", e.getMessage());
        }

        // 尝试查找带 profile 的配置文件
        if (!activeProfile.isEmpty()) {
            try {
                java.net.URL resource =
                    getClass().getClassLoader().getResource(configName + "-" + activeProfile + ".yml");
                if (resource != null && "file".equals(resource.getProtocol())) {
                    Path path = Paths.get(resource.toURI());
                    if (java.nio.file.Files.exists(path)) {
                        return path;
                    }
                }
            } catch (Exception e) {
                log.debug("从类路径查找带 profile 的配置文件失败: {}", e.getMessage());
            }
        }

        // 尝试从当前工作目录查找
        String[] possiblePaths = {configName + ".yml", configName + ".yaml",
            "src/main/resources/" + configName + ".yml", "src/main/resources/" + configName + ".yaml",};

        for (String pathStr : possiblePaths) {
            Path path = Paths.get(pathStr);
            if (java.nio.file.Files.exists(path)) {
                return path;
            }
        }

        log.warn("未找到配置文件: {}.yml (profile: {})", configName, activeProfile);
        return null;
    }

    /**
     * 监听配置文件变化
     */
    private void watchConfigFile() {
        log.info("配置文件监听线程已启动");
        while (running) {
            try {
                WatchKey key = watchService.take();
                if (key == null) {
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    Path fileName = ev.context();

                    // 检查是否是配置文件
                    if (fileName.toString().endsWith(".yml") || fileName.toString().endsWith(".yaml")) {
                        log.info("检测到配置文件变化: {}", fileName);

                        // 延迟一小段时间，避免文件写入未完成
                        Thread.sleep(properties.getDelayMillis());

                        // 触发配置刷新
                        try {
                            log.info("开始刷新配置...");
                            contextRefresher.refresh();
                            log.info("配置刷新完成");
                        } catch (Exception e) {
                            log.error("配置刷新失败", e);
                        }
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    log.warn("WatchKey 无效，停止监听");
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("配置文件监听线程被中断");
                break;
            } catch (Exception e) {
                log.error("监听配置文件时发生异常", e);
                // 发生异常时等待一段时间再继续
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("配置文件监听线程已退出");
    }
}
