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

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.deser.jdk.StringDeserializer;

import top.sephy.infra.jackson.annotation.XSSIgnore;

/**
 * Jackson 3 版本的 AbstractXSSDeserializer
 */
public abstract class AbstractXSSDeserializer3 extends StdDeserializer<String> implements ValueInstantiator.Gettable {

    public AbstractXSSDeserializer3() {
        super(StringDeserializer.instance);
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {

        if (property != null) {
            XSSIgnore annotation = property.getAnnotation(XSSIgnore.class);
            if (annotation != null && annotation.ignore()) {
                return StringDeserializer.instance;
            }
        }

        return this;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        return doDeserialize(p, ctxt);
    }

    protected abstract String doDeserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException;
}
