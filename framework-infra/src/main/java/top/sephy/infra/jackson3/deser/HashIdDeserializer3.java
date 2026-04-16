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

import org.hashids.Hashids;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.jdk.NumberDeserializers;
import tools.jackson.databind.deser.std.StdDeserializer;

import top.sephy.infra.jackson.annotation.JsonHashId;

/**
 * Jackson 3 版本的 HashIdDeserializer
 */
public class HashIdDeserializer3 extends StdDeserializer<Long> {

    private Hashids hashids;

    public HashIdDeserializer3(Hashids hashids) {
        super(Long.class);
        this.hashids = hashids;
    }

    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String text = p.getText();
        long[] decode = hashids.decode(text);
        if (decode.length > 0) {
            return decode[0];
        }
        return null;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (property == null) {
            return NumberDeserializers.find(Long.class);
        }
        JsonHashId jsonHashId = property.getAnnotation(JsonHashId.class);
        if (jsonHashId != null) {
            String salt = jsonHashId.salt();
            Hashids hashIds = new Hashids(salt);
            return new HashIdDeserializer3(hashIds);
        }
        return NumberDeserializers.find(Long.class);
    }
}
