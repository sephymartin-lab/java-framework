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

import org.hashids.Hashids;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.jdk.NumberSerializers;
import tools.jackson.databind.ser.std.StdSerializer;

import lombok.NonNull;
import top.sephy.infra.jackson.annotation.JsonHashId;

/**
 * Jackson 3 版本的 HashIdSerializer
 */
public class HashIdSerializer3 extends StdSerializer<Long> {

    private Hashids hashids;

    public HashIdSerializer3(@NonNull Hashids hashids) {
        super(Long.class);
        this.hashids = hashids;
    }

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(hashids.encode(value));
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
        if (property != null) {
            JsonHashId jsonHashId = property.getAnnotation(JsonHashId.class);
            if (jsonHashId != null) {
                String salt = jsonHashId.salt();
                Hashids hashIds = new Hashids(salt);
                return new HashIdSerializer3(hashIds);
            }
        }
        return new NumberSerializers.LongSerializer(Long.class);
    }
}
