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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Jackson 3 版本的 LocalDateTime序列化规则
 * <p>
 * 会将LocalDateTime序列化为毫秒级时间戳
 */
public class CustomLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {
    private static final long serialVersionUID = -3352134288289600043L;

    public static final CustomLocalDateTimeSerializer INSTANCE =
        new CustomLocalDateTimeSerializer(null, null, JsonFormat.Shape.NUMBER_INT);

    private final Boolean _useTimestamp;

    private final DateTimeFormatter _formatter;

    private final JsonFormat.Shape _shape;

    private CustomLocalDateTimeSerializer(Boolean useTimestamp, DateTimeFormatter dtf, JsonFormat.Shape shape) {
        super(LocalDateTime.class);
        _useTimestamp = useTimestamp;
        _formatter = dtf;
        _shape = shape;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
        JsonFormat.Value format = findFormatOverrides(prov, property, handledType());
        if (format != null) {
            Boolean useTimestamp = null;

            // Simple case first: serialize as numeric timestamp?
            JsonFormat.Shape shape = format.getShape();
            if (shape == JsonFormat.Shape.ARRAY || shape.isNumeric()) {
                useTimestamp = Boolean.TRUE;
            } else {
                useTimestamp = (shape == JsonFormat.Shape.STRING) ? Boolean.FALSE : null;
            }
            DateTimeFormatter dtf = _formatter;

            // If not, do we have a pattern?
            if (format.hasPattern()) {
                dtf = _useDateTimeFormatter(prov, format);
            }
            CustomLocalDateTimeSerializer ser = this;
            if ((shape != _shape) || (useTimestamp != _useTimestamp) || (dtf != _formatter)) {
                ser = ser.withFormat(useTimestamp, dtf, shape);
            }
            Boolean writeZoneId = format.getFeature(JsonFormat.Feature.WRITE_DATES_WITH_ZONE_ID);
            Boolean writeNanoseconds = format.getFeature(JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
            if ((writeZoneId != null) || (writeNanoseconds != null)) {
                ser = ser.withFeatures(writeZoneId, writeNanoseconds);
            }
            return ser;
        }
        return this;
    }

    protected JsonFormat.Value findFormatOverrides(SerializationContext provider, BeanProperty prop,
        Class<?> typeForDefaults) {
        if (prop != null) {
            return prop.findPropertyFormat(provider.getConfig(), typeForDefaults);
        }
        // even without property or AnnotationIntrospector, may have type-specific
        // defaults
        return provider.getDefaultPropertyFormat(typeForDefaults);
    }

    protected DateTimeFormatter _useDateTimeFormatter(SerializationContext prov, JsonFormat.Value format) {
        DateTimeFormatter dtf;
        final String pattern = format.getPattern();
        final Locale locale = format.hasLocale() ? format.getLocale() : prov.getLocale();
        if (locale == null) {
            dtf = DateTimeFormatter.ofPattern(pattern);
        } else {
            dtf = DateTimeFormatter.ofPattern(pattern, locale);
        }
        // Issue #69: For instant serializers/deserializers we need to configure the
        // formatter with
        // a time zone picked up from JsonFormat annotation, otherwise serialization
        // might not work
        if (format.hasTimeZone()) {
            dtf = dtf.withZone(format.getTimeZone().toZoneId());
        }
        return dtf;
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator g, SerializationContext provider) {
        if (useTimestamp(provider)) {
            g.writeNumber(value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        } else {
            DateTimeFormatter dtf = _formatter;
            if (dtf == null) {
                dtf = _defaultFormatter();
            }
            g.writeString(value.format(dtf));
        }
    }

    protected boolean useTimestamp(SerializationContext provider) {
        if (_useTimestamp != null) {
            return _useTimestamp.booleanValue();
        }
        if (_shape != null) {
            if (_shape == JsonFormat.Shape.STRING) {
                return false;
            }
            if (_shape == JsonFormat.Shape.NUMBER_INT) {
                return true;
            }
        }
        // assume that explicit formatter definition implies use of textual format
        return (_formatter == null) && (provider != null) && provider.isEnabled(getTimestampsFeature());
    }

    protected DateTimeFeature getTimestampsFeature() {
        return DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS;
    }

    protected DateTimeFormatter _defaultFormatter() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    }

    CustomLocalDateTimeSerializer withFormat(Boolean useTimestamp, DateTimeFormatter f, JsonFormat.Shape shape) {
        return new CustomLocalDateTimeSerializer(useTimestamp, f, shape);
    }

    CustomLocalDateTimeSerializer withFeatures(Boolean writeZoneId, Boolean writeNanoseconds) {
        return new CustomLocalDateTimeSerializer(_useTimestamp, _formatter, _shape);
    }
}
