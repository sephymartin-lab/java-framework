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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 测试用 @RefreshScope Bean，用于验证配置刷新功能
 * <p>
 * 使用 @Value 直接注入配置值，配合 @RefreshScope 实现配置热更新
 */
@Component
@RefreshScope
public class TestRefreshableService {

    @Value("${test.message:default}")
    private String message;

    /**
     * 获取当前配置的消息
     * 
     * @return 配置中的消息
     */
    public String getMessage() {
        return message;
    }
}
