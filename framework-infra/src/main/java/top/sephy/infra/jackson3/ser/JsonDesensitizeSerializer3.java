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
package top.sephy.infra.jackson3.ser;

import java.io.Serial;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

import lombok.Setter;
import top.sephy.infra.jackson.annotation.JsonDesensitize;
import top.sephy.infra.security.DesensitizationStrategy;

/**
 * Jackson 3 版本的 JsonDesensitizeSerializer
 */
@Setter
public class JsonDesensitizeSerializer3 extends StdSerializer<String> {

    @Serial
    private static final long serialVersionUID = -2517170909648829158L;
    private DesensitizationStrategy strategy;

    public JsonDesensitizeSerializer3() {
        super(String.class);
    }

    private JsonDesensitizeSerializer3(DesensitizationStrategy strategy) {
        super(String.class);
        this.strategy = strategy;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
        ValueSerializer<?> ser = prov.findValueSerializer(String.class);

        if (property == null) {
            return ser;
        }
        JsonDesensitize annotation = property.getAnnotation(JsonDesensitize.class);
        if (annotation != null && annotation.value() != null) {
            ser = new JsonDesensitizeSerializer3(annotation.value());
        }

        return ser;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext provider) {
        if (strategy != null) {
            gen.writeString(strategy.desensitize(value));
        } else {
            gen.writeString(value);
        }
    }
}
