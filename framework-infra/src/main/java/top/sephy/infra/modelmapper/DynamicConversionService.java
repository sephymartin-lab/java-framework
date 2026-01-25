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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.modelmapper.ModelMapper;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * 支持懒加载动态注册的 ConversionService 实现
 * <p>
 * 首次调用 convert 时自动注册转换器到内部的 ConversionService，
 * 后续调用直接使用缓存的转换器。
 * </p>
 *
 * <p>特性：</p>
 * <ul>
 *     <li>零配置 - 无需预先注册任何类型对</li>
 *     <li>完全解耦 - DO 和 DTO 无任何依赖关系</li>
 *     <li>Spring 兼容 - 实现标准 ConversionService 接口</li>
 *     <li>线程安全 - 使用 ConcurrentHashMap 和双重检查锁</li>
 *     <li>按需注册 - 只有实际使用的类型对才会被注册</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Autowired
 * private ConversionService conversionService;
 *
 * // 无需任何预注册，直接使用
 * SysUserDTO dto = conversionService.convert(userDO, SysUserDTO.class);
 * }</pre>
 *
 * @author sephy
 */
public class DynamicConversionService implements ConversionService {

    private final ModelMapper modelMapper;
    private final ConfigurableConversionService delegate;
    private final Set<String> registeredPairs = ConcurrentHashMap.newKeySet();

    /**
     * 使用指定的 ModelMapper 创建 DynamicConversionService
     *
     * @param modelMapper ModelMapper 实例
     */
    public DynamicConversionService(ModelMapper modelMapper) {
        this(modelMapper, new DefaultConversionService());
    }

    /**
     * 使用指定的 ModelMapper 和 ConversionService 创建 DynamicConversionService
     *
     * @param modelMapper ModelMapper 实例
     * @param delegate 委托的 ConfigurableConversionService 实例
     */
    public DynamicConversionService(ModelMapper modelMapper, ConfigurableConversionService delegate) {
        this.modelMapper = modelMapper;
        this.delegate = delegate;
    }

    @Override
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
        // 始终返回 true，因为 ModelMapper 可以处理任意类型转换
        // 如果转换失败，会在 convert 方法中抛出异常
        return true;
    }

    @Override
    public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
        // 始终返回 true，因为 ModelMapper 可以处理任意类型转换
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(Object source, Class<T> targetType) {
        if (source == null) {
            return null;
        }

        Class<?> sourceType = source.getClass();
        String key = buildKey(sourceType, targetType);

        // 首次调用时自动注册转换器
        if (!registeredPairs.contains(key)) {
            registerConverter(sourceType, targetType, key);
        }

        return delegate.convert(source, targetType);
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }

        Class<?> sourceClass = sourceType.getType();
        Class<?> targetClass = targetType.getType();
        String key = buildKey(sourceClass, targetClass);

        // 首次调用时自动注册转换器
        if (!registeredPairs.contains(key)) {
            registerConverter(sourceClass, targetClass, key);
        }

        return delegate.convert(source, sourceType, targetType);
    }

    /**
     * 注册转换器（线程安全）
     */
    private <S, T> void registerConverter(Class<?> sourceType, Class<?> targetType, String key) {
        synchronized (this) {
            if (!registeredPairs.contains(key)) {
                // 使用 ModelMapper 进行实际转换
                @SuppressWarnings("unchecked")
                Class<S> srcType = (Class<S>) sourceType;
                @SuppressWarnings("unchecked")
                Class<T> tgtType = (Class<T>) targetType;
                delegate.addConverter(srcType, tgtType, source -> modelMapper.map(source, tgtType));
                registeredPairs.add(key);
            }
        }
    }

    /**
     * 构建类型对的唯一键
     */
    private String buildKey(Class<?> sourceType, Class<?> targetType) {
        return sourceType.getName() + "->" + targetType.getName();
    }

    /**
     * 获取内部的 ModelMapper 实例
     *
     * @return ModelMapper 实例
     */
    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    /**
     * 获取内部的 ConversionService 委托
     *
     * @return ConfigurableConversionService 实例
     */
    public ConfigurableConversionService getDelegate() {
        return delegate;
    }

    /**
     * 获取已注册的类型对数量
     *
     * @return 已注册的类型对数量
     */
    public int getRegisteredPairsCount() {
        return registeredPairs.size();
    }

    /**
     * 检查指定的类型对是否已注册
     *
     * @param sourceType 源类型
     * @param targetType 目标类型
     * @return 如果已注册返回 true
     */
    public boolean isRegistered(Class<?> sourceType, Class<?> targetType) {
        return registeredPairs.contains(buildKey(sourceType, targetType));
    }
}
