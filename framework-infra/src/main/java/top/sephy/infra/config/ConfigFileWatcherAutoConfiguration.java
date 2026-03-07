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

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 配置文件监听器自动配置类
 * 
 * @author sephy
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.config.file-watcher", name = "enabled", havingValue = "true",
    matchIfMissing = false)
@EnableConfigurationProperties(ConfigFileWatcherProperties.class)
public class ConfigFileWatcherAutoConfiguration {

    @Bean
    @ConditionalOnBean(ContextRefresher.class)
    public ConfigFileWatcher configFileWatcher(ContextRefresher contextRefresher, Environment environment,
        ConfigFileWatcherProperties properties) {
        return new ConfigFileWatcher(contextRefresher, environment, properties);
    }
}
