/*
 * Copyright 2022-2026 sephy.top
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.sephy.infra.modelmapper;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ModelMapper 配置属性
 * <p>
 * 通过 Spring Boot 配置文件进行配置，配置前缀为 {@code infra.modelmapper}
 * </p>
 *
 * <p>配置示例：</p>
 * <pre>{@code
 * infra:
 *   modelmapper:
 *     matching-strategy: STRICT
 *     field-matching-enabled: true
 *     ambiguity-ignored: true
 *     skip-null-enabled: true
 * }</pre>
 *
 * @author sephy
 */
@ConfigurationProperties(prefix = "infra.modelmapper")
public class ModelMapperProperties {

    /**
     * 匹配策略
     * <ul>
     *     <li>STANDARD - 标准匹配，允许一定程度的模糊匹配</li>
     *     <li>STRICT - 严格匹配，源和目标属性名必须完全匹配</li>
     *     <li>LOOSE - 宽松匹配，允许更大程度的模糊匹配</li>
     * </ul>
     * 默认为 STRICT，推荐使用严格模式避免意外映射
     */
    private MatchingStrategy matchingStrategy = MatchingStrategy.STRICT;

    /**
     * 是否启用字段匹配（包括私有字段）
     * <p>
     * 启用后可以直接访问私有字段，不依赖 getter/setter
     * </p>
     */
    private boolean fieldMatchingEnabled = true;

    /**
     * 是否忽略歧义映射
     * <p>
     * 当存在多个可能的源属性匹配目标属性时，忽略歧义而不抛出异常
     * </p>
     */
    private boolean ambiguityIgnored = true;

    /**
     * 是否跳过 null 值
     * <p>
     * 启用后，源对象中的 null 值不会覆盖目标对象中的现有值
     * </p>
     */
    private boolean skipNullEnabled = true;

    /**
     * 匹配策略枚举
     */
    public enum MatchingStrategy {
        /**
         * 标准匹配策略
         */
        STANDARD,
        /**
         * 严格匹配策略
         */
        STRICT,
        /**
         * 宽松匹配策略
         */
        LOOSE
    }

    public MatchingStrategy getMatchingStrategy() {
        return matchingStrategy;
    }

    public void setMatchingStrategy(MatchingStrategy matchingStrategy) {
        this.matchingStrategy = matchingStrategy;
    }

    public boolean isFieldMatchingEnabled() {
        return fieldMatchingEnabled;
    }

    public void setFieldMatchingEnabled(boolean fieldMatchingEnabled) {
        this.fieldMatchingEnabled = fieldMatchingEnabled;
    }

    public boolean isAmbiguityIgnored() {
        return ambiguityIgnored;
    }

    public void setAmbiguityIgnored(boolean ambiguityIgnored) {
        this.ambiguityIgnored = ambiguityIgnored;
    }

    public boolean isSkipNullEnabled() {
        return skipNullEnabled;
    }

    public void setSkipNullEnabled(boolean skipNullEnabled) {
        this.skipNullEnabled = skipNullEnabled;
    }
}
