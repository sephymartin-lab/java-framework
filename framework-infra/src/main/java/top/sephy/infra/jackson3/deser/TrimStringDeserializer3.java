/*
 * Copyright 2022-2025 sephy.top
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
package top.sephy.infra.jackson3.deser;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.deser.jdk.StringDeserializer;

import top.sephy.infra.jackson.annotation.KeepSpaces;

/**
 * Jackson 3 版本的 String 反序列化器，自动去除首尾空格
 * <p>
 * 如果字段标注了 {@link KeepSpaces} 注解，则保留原始字符串不做 trim 处理
 * </p>
 */
public class TrimStringDeserializer3 extends StdDeserializer<String> {

    @Serial
    private static final long serialVersionUID = -1234567890123456789L;

    public static final TrimStringDeserializer3 INSTANCE = new TrimStringDeserializer3();

    public TrimStringDeserializer3() {
        super(String.class);
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {

        if (property != null) {
            KeepSpaces annotation = property.getAnnotation(KeepSpaces.class);
            if (annotation != null) {
                // 如果标注了 @KeepSpaces 注解，则使用默认的 StringDeserializer，不做 trim
                return StringDeserializer.instance;
            }
        }

        return this;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String value = p.getValueAsString();
        // 使用 Apache Commons Lang 的 trim 方法，会处理各种空白字符
        return StringUtils.trim(value);
    }
}
