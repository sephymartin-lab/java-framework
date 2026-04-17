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
package top.sephy.infra.utils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hashids.Hashids;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;
import top.sephy.infra.jackson3.deser.HashIdDeserializer3;
import top.sephy.infra.jackson3.ser.CustomLocalDateTimeSerializer;
import top.sephy.infra.jackson3.ser.HashIdSerializer3;
import tools.jackson.core.JacksonException;
import tools.jackson.core.Version;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

@Slf4j
public abstract class JacksonUtils {

    private static ObjectMapper DEFAULT_OBJECT_MAPPER = newDefaultObjectMapper();

    private static ObjectMapper DEFAULT_OBJECT_MAPPER_INCLUDE_NULL;

    // private static boolean javaTimeModulePresent = false;

    private static boolean playwrightModulePresent = false;

    static {
        ClassLoader classLoader = JacksonUtils.class.getClassLoader();
        // javaTimeModulePresent =
        // ClassUtils.isPresent("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule",
        // classLoader);
        playwrightModulePresent = ClassUtils.isPresent("com.microsoft.playwright.Playwright", classLoader);
        DEFAULT_OBJECT_MAPPER_INCLUDE_NULL = newDefaultObjectMapper();
        DEFAULT_OBJECT_MAPPER_INCLUDE_NULL = DEFAULT_OBJECT_MAPPER_INCLUDE_NULL.rebuild()
            .changeDefaultPropertyInclusion(oldValue -> oldValue.withValueInclusion(JsonInclude.Include.ALWAYS))
            .build();
    }

    private static TypeReference<HashMap<String, String>> STRING_MAP = new TypeReference<HashMap<String, String>>() {};

    public static ObjectMapper newDefaultObjectMapper() {
        SimpleModule module = new SimpleModule("JacksonUtilsModule")
            .addSerializer(LocalDateTime.class, CustomLocalDateTimeSerializer.INSTANCE)
            .addDeserializer(LocalDateTime.class, EpochMillisLocalDateTimeDeserializer.INSTANCE)
            .addSerializer(BigDecimal.class, CustomBigDecimalSerializer3.INSTANCE)
            .addSerializer(LocalDate.class, CustomLocalDateSerializer3.INSTANCE)
            .addDeserializer(LocalDate.class, EpochMillisLocalDateDeserializer.INSTANCE)
            .addSerializer(Long.class, new HashIdSerializer3(new Hashids()))
            .addDeserializer(Long.class, new HashIdDeserializer3(new Hashids()));
        JsonMapper.Builder builder = JsonMapper.builder()
            .addModule(module)
            .changeDefaultPropertyInclusion(oldValue -> oldValue.withValueInclusion(JsonInclude.Include.NON_NULL))
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        if (playwrightModulePresent) {
            builder.addModule(new PlaywrightModule());
        }
        return builder.build();
    }

    private static void disableFeatures(ObjectMapper objectMapper) {
        objectMapper.rebuild()
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    }

    private static void enableFeatures(ObjectMapper objectMapper) {}

    public static String toJson(Object object) {
        return toJson(DEFAULT_OBJECT_MAPPER, object);
    }

    public static String toJson(ObjectMapper objectMapper, Object object) {
        assertObjectMapper(objectMapper);
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new JsonException(e);
        }
    }

    public static <T> T jsonToObject(String json, Class<T> clazz) {
        return jsonToObject(DEFAULT_OBJECT_MAPPER, json, clazz);
    }

    public static <T> T jsonToObject(String json, TypeReference<T> typeReference) {
        return jsonToObject(DEFAULT_OBJECT_MAPPER, json, typeReference);
    }

    public static <T> T jsonToObject(ObjectMapper objectMapper, String json, Class<T> clazz) {
        assertObjectMapper(objectMapper);
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public static <T> T jsonToObject(ObjectMapper objectMapper, String json, TypeReference<T> typeReference) {
        assertObjectMapper(objectMapper);
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public static <T> T jsonToObject(InputStream inputStream, Class<T> clazz) {
        return jsonToObject(DEFAULT_OBJECT_MAPPER, inputStream, clazz);
    }

    public static <T> T jsonToObject(ObjectMapper objectMapper, InputStream inputStream, Class<T> clazz) {
        assertObjectMapper(objectMapper);
        try {
            return objectMapper.readValue(inputStream, clazz);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public static <T> List<T> jsonToList(String json, Class<T> clazz) {
        return jsonToList(DEFAULT_OBJECT_MAPPER, json, clazz);
    }

    public static <T> List<T> jsonToList(ObjectMapper objectMapper, String json, Class<T> clazz) {
        assertObjectMapper(objectMapper);
        try {
            return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
            // return objectMapper.readValue(json, new TypeReference<List<T>>() {
            // });
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public static JsonNode jsonToTree(String json) {
        return jsonToTree(DEFAULT_OBJECT_MAPPER, json);
    }

    public static JsonNode jsonToTree(ObjectMapper objectMapper, String json) {
        assertObjectMapper(objectMapper);
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public static <T> T treeToValue(JsonNode jsonNode, Class<T> clazz) {
        return treeToValue(DEFAULT_OBJECT_MAPPER, jsonNode, clazz);
    }

    public static <T> T treeToValue(ObjectMapper objectMapper, JsonNode jsonNode, Class<T> clazz) {
        assertObjectMapper(objectMapper);
        try {
            return objectMapper.treeToValue(jsonNode, clazz);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public static Map<String, Object> convertToMap(Object object) {
        return convertToMap(DEFAULT_OBJECT_MAPPER, object);
    }

    public static Map<String, Object> convertToMapIncludeNull(Object object) {
        return convertToMap(DEFAULT_OBJECT_MAPPER_INCLUDE_NULL, object);
    }

    public static Map<String, Object> convertToMap(ObjectMapper objectMapper, Object object) {
        assertObjectMapper(objectMapper);
        return objectMapper.convertValue(object, Map.class);
    }

    public static Map<String, String> convertToStringMap(Object object) {
        return convertToStringMap(DEFAULT_OBJECT_MAPPER, object);
    }

    public static Map<String, String> convertToStringMapIncludeNull(Object object) {
        return convertToStringMap(DEFAULT_OBJECT_MAPPER_INCLUDE_NULL, object);
    }

    public static Map<String, String> convertToStringMap(ObjectMapper objectMapper, Object object) {
        assertObjectMapper(objectMapper);
        return objectMapper.convertValue(object, STRING_MAP);
    }

    public static <E> E convert(Object from, Class<E> to) {
        return convert(DEFAULT_OBJECT_MAPPER, from, to);
    }

    public static <E> E convert(ObjectMapper objectMapper, Object from, Class<E> to) {
        assertObjectMapper(objectMapper);
        return objectMapper.convertValue(from, to);
    }

    public static <E> E stringMapToObject(Map<String, String> map, Class<E> clazz) {
        return stringMapToObject(DEFAULT_OBJECT_MAPPER, map, clazz);
    }

    public static <E> E stringMapToObject(ObjectMapper objectMapper, Map<String, String> map, Class<E> clazz) {
        return objectMapper.convertValue(map, clazz);
    }

    private static void assertObjectMapper(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper, "ObjectMapper must not be null.");
    }

    public static class JsonException extends RuntimeException {

        private static long serialVersionUID = -8318031819390714507L;

        public JsonException() {}

        public JsonException(String message) {
            super(message);
        }

        public JsonException(String message, Throwable cause) {
            super(message, cause);
        }

        public JsonException(Throwable cause) {
            super(cause);
        }

        public JsonException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

    public static class PlaywrightModule extends SimpleModule {
        public PlaywrightModule() {
            super("PlaywrightModule", new Version(0, 0, 1, null, null, null));
        }

        @Override
        public void setupModule(SetupContext context) {
            context.setMixIn(com.microsoft.playwright.options.ViewportSize.class, ViewportSizeMixIn.class);
            context.setMixIn(com.microsoft.playwright.options.ScreenSize.class, ScreenSizeMixIn.class);
            context.setMixIn(com.microsoft.playwright.options.Cookie.class, CookieMixIn.class);
        }
    }

    private static final class CustomBigDecimalSerializer3 extends StdSerializer<BigDecimal> {

        private static final int CURRENCY_SCALE = 2;

        private static final CustomBigDecimalSerializer3 INSTANCE = new CustomBigDecimalSerializer3(null);

        private final DecimalFormat decimalFormat;

        private CustomBigDecimalSerializer3(DecimalFormat decimalFormat) {
            super(BigDecimal.class);
            this.decimalFormat = decimalFormat;
        }

        @Override
        public void serialize(BigDecimal value, tools.jackson.core.JsonGenerator gen, SerializationContext serializers) {
            if (value == null) {
                gen.writeNull();
                return;
            }
            if (decimalFormat != null) {
                gen.writeNumber(decimalFormat.format(value));
                return;
            }
            if (value.scale() < CURRENCY_SCALE) {
                gen.writeString(value.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP).toPlainString());
                return;
            }
            gen.writeString(value.toPlainString());
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext prov, tools.jackson.databind.BeanProperty property) {
            if (property == null) {
                return this;
            }
            com.fasterxml.jackson.annotation.JsonFormat.Value format =
                property.findPropertyFormat(prov.getConfig(), handledType());
            if (format == null || !format.hasPattern()) {
                return this;
            }
            DecimalFormat customFormat = new DecimalFormat(format.getPattern());
            customFormat.setRoundingMode(RoundingMode.HALF_UP);
            return new CustomBigDecimalSerializer3(customFormat);
        }
    }

    private static final class CustomLocalDateSerializer3 extends StdSerializer<LocalDate> {

        private static final CustomLocalDateSerializer3 INSTANCE = new CustomLocalDateSerializer3();

        private CustomLocalDateSerializer3() {
            super(LocalDate.class);
        }

        @Override
        public void serialize(LocalDate value, tools.jackson.core.JsonGenerator gen, SerializationContext provider) {
            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeNumber(value.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
    }

    private static final class EpochMillisLocalDateDeserializer extends LocalDateDeserializer {

        private static final EpochMillisLocalDateDeserializer INSTANCE =
            new EpochMillisLocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE);

        private EpochMillisLocalDateDeserializer(DateTimeFormatter formatter) {
            super(formatter);
        }

        @Override
        public LocalDate deserialize(tools.jackson.core.JsonParser p, DeserializationContext ctxt)
            throws JacksonException {
            if (p.hasToken(tools.jackson.core.JsonToken.VALUE_NUMBER_INT)) {
                return Instant.ofEpochMilli(p.getLongValue()).atZone(ZoneId.systemDefault()).toLocalDate();
            }
            return super.deserialize(p, ctxt);
        }
    }

    private static final class EpochMillisLocalDateTimeDeserializer extends LocalDateTimeDeserializer {

        private static final EpochMillisLocalDateTimeDeserializer INSTANCE =
            new EpochMillisLocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        private EpochMillisLocalDateTimeDeserializer(DateTimeFormatter formatter) {
            super(formatter);
        }

        @Override
        public LocalDateTime deserialize(tools.jackson.core.JsonParser p, DeserializationContext ctxt)
            throws JacksonException {
            if (p.hasToken(tools.jackson.core.JsonToken.VALUE_NUMBER_INT)) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(p.getLongValue()), ZoneId.systemDefault());
            }
            return super.deserialize(p, ctxt);
        }
    }

    @Slf4j
    public static class ViewportSizeMixIn {
        @JsonCreator
        public ViewportSizeMixIn(@JsonProperty("width") int width, @JsonProperty("height") int height) {
            log.info("ViewportSizeMixIn called!");
        }
    }

    @Slf4j
    public static class ScreenSizeMixIn {
        @JsonCreator
        public ScreenSizeMixIn(@JsonProperty("width") int width, @JsonProperty("height") int height) {
            log.info("ViewportSizeMixIn called!");
        }
    }

    @Slf4j
    public static class CookieMixIn {
        @JsonCreator
        public CookieMixIn(@JsonProperty("name") String name, @JsonProperty("value") String value) {
            log.info("CookieMixIn called!");
        }
    }
}
