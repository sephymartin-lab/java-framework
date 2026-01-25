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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;

/**
 * 基于 ModelMapper 的 BeanConverter 实现
 * <p>
 * 封装 ModelMapper 实例，提供统一的对象转换能力
 * </p>
 *
 * @author sephy
 */
public class ModelMapperBeanConverter implements BeanConverter {

    private final ModelMapper modelMapper;

    public ModelMapperBeanConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public <T> T convert(Object source, Class<T> targetType) {
        if (source == null) {
            return null;
        }
        return modelMapper.map(source, targetType);
    }

    @Override
    public <T> List<T> convertList(Collection<?> sources, Class<T> targetType) {
        if (sources == null || sources.isEmpty()) {
            return Collections.emptyList();
        }
        return sources.stream()
            .map(source -> convert(source, targetType))
            .collect(Collectors.toList());
    }

    @Override
    public void copyProperties(Object source, Object destination) {
        if (source != null && destination != null) {
            modelMapper.map(source, destination);
        }
    }

    /**
     * 获取内部的 ModelMapper 实例，用于自定义配置
     *
     * @return ModelMapper 实例
     */
    public ModelMapper getModelMapper() {
        return modelMapper;
    }
}
