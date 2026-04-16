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
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

/**
 * Jackson 3 版本的 JsoupXssDeserializer
 */
public class JsoupXssDeserializer3 extends AbstractXSSDeserializer3 {

    @Serial
    private static final long serialVersionUID = 3444056323052247564L;

    public static JsoupXssDeserializer3 INSTANCE = new JsoupXssDeserializer3();

    @Override
    protected String doDeserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String val = StringUtils.trim(p.getValueAsString());
        return Jsoup.clean(val, Whitelist.none());
    }
}
