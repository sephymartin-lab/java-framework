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
package top.sephy.infra.option;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.NonNull;

public class CompositeCachedDictEntryProvider implements ApplicationListener<ContextRefreshedEvent> {

    private final ObjectProvider<MultiDictEntryListProvider> multiListobjectProvider;

    private final ObjectProvider<DictEntryListProvider> singleListProvider;

    private final Map<String, List<DictEntry<Object, Object>>> dictCache = new ConcurrentHashMap<>();

    public CompositeCachedDictEntryProvider(ObjectProvider<MultiDictEntryListProvider> multiListobjectProvider,
        ObjectProvider<DictEntryListProvider> singleListProvider) {
        this.multiListobjectProvider = multiListobjectProvider;
        this.singleListProvider = singleListProvider;
    }

    public Set<String> types() {
        return dictCache.keySet();
    }

    public Map<String, List<DictEntry<Object, Object>>> optionsMap() {
        return ImmutableMap.copyOf(dictCache);
    }

    public List<DictEntry<Object, Object>> getOptionsByType(String type) {
        return dictCache.getOrDefault(type, Collections.emptyList());
    }

    public DictEntry<Object, Object> lookUpOption(@NonNull String type, @NonNull Object valueToLookUp,
        boolean compareWithString, boolean caseSensitive) {
        List<DictEntry<Object, Object>> options = getOptionsByType(type);
        for (DictEntry<Object, Object> option : options) {
            Object valueToCompare = option.getValue();
            if (compareWithString) {
                String k1 = String.valueOf(valueToLookUp);
                String k2 = String.valueOf(valueToCompare);
                if (caseSensitive && StringUtils.equals(k1, k2)) {
                    return option;
                } else if (!caseSensitive && StringUtils.equalsIgnoreCase(k1, k2)) {
                    return option;
                }
            } else if (Objects.equals(valueToLookUp, valueToCompare)) {
                return option;
            }
        }
        return null;
    }

    public String getLabel(@NonNull String type, @NonNull Object valueToLookUp, boolean compareWithString,
        boolean caseSensitive, String defaultValue) {
        DictEntry<Object, Object> option = lookUpOption(type, valueToLookUp, compareWithString, caseSensitive);
        if (option != null && option.getLabel() != null) {
            return String.valueOf(option.getLabel());
        }
        return defaultValue;
    }

    public void refresh() {
        Iterator<MultiDictEntryListProvider> iterator = multiListobjectProvider.stream().iterator();
        singleListProvider.stream().forEach(provider -> dictCache.put(provider.getType(), provider.getOptions()));
        while (iterator.hasNext()) {
            MultiDictEntryListProvider<Object, Object> provider = iterator.next();
            Map<String, List<DictEntry<Object, Object>>> tmp = provider.optionsMap();
            for (Map.Entry<String, List<DictEntry<Object, Object>>> entry : tmp.entrySet()) {
                dictCache.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
            }
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        refresh();
    }
}
