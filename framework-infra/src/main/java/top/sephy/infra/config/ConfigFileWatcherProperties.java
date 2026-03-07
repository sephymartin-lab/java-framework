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

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 配置文件监听器配置属性
 * 
 * @author sephy
 */
@Data
@ConfigurationProperties(prefix = "spring.config.file-watcher")
public class ConfigFileWatcherProperties {

    /**
     * 是否启用配置文件监听功能，默认关闭
     */
    private boolean enabled = false;

    /**
     * 文件变化后延迟刷新时间（毫秒），默认 500ms
     * 用于避免文件写入未完成时触发刷新
     */
    private long delayMillis = 500;
}
