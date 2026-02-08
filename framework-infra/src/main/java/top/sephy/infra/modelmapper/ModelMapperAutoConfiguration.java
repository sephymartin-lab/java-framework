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

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * ModelMapper 自动配置类
 * <p>
 * 提供 ModelMapper 的自动装配能力，包括：
 * <ul>
 *     <li>ModelMapper Bean 的创建和配置</li>
 *     <li>BeanConverter Bean 的创建</li>
 *     <li>自动扫描并应用 ModelMapperConverterRegistrar</li>
 *     <li>与 Spring ConversionService 的集成</li>
 * </ul>
 * </p>
 *
 * @author sephy
 */
@Configuration
@ConditionalOnClass(ModelMapper.class)
@EnableConfigurationProperties(ModelMapperProperties.class)
public class ModelMapperAutoConfiguration {

    /**
     * 创建并配置 ModelMapper Bean
     * <p>
     * 根据 ModelMapperProperties 配置 ModelMapper 的行为，
     * 并自动应用所有 ModelMapperConverterRegistrar 实现的自定义配置
     * </p>
     *
     * @param properties 配置属性
     * @param registrarsProvider ModelMapperConverterRegistrar 提供者
     * @return 配置好的 ModelMapper 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ModelMapper modelMapper(ModelMapperProperties properties,
            ObjectProvider<List<ModelMapperConverterRegistrar>> registrarsProvider) {
        ModelMapper modelMapper = new ModelMapper();

        // 配置匹配策略
        configureMatchingStrategy(modelMapper, properties);

        // 配置其他选项
        modelMapper.getConfiguration()
            .setFieldMatchingEnabled(properties.isFieldMatchingEnabled())
            .setAmbiguityIgnored(properties.isAmbiguityIgnored())
            .setSkipNullEnabled(properties.isSkipNullEnabled());

        // 应用自定义注册器
        List<ModelMapperConverterRegistrar> registrars = registrarsProvider.getIfAvailable();
        if (registrars != null) {
            for (ModelMapperConverterRegistrar registrar : registrars) {
                registrar.configure(modelMapper);
            }
        }

        return modelMapper;
    }

    /**
     * 创建 BeanConverter Bean
     *
     * @param modelMapper ModelMapper 实例
     * @return BeanConverter 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public BeanConverter beanConverter(ModelMapper modelMapper) {
        return new ModelMapperBeanConverter(modelMapper);
    }

    /**
     * 创建 DynamicConversionService Bean
     * <p>
     * 支持懒加载动态注册的 ConversionService 实现，首次调用 convert 时自动注册转换器。
     * 使用特定的 Bean 名称避免与 Spring 内置的 ConversionService 冲突。
     * </p>
     * <p>
     * 注入方式：
     * <pre>{@code
     * // 方式 1: 使用 @Qualifier
     * @Resource
     * @Qualifier("modelMapperConversionService")
     * private ConversionService conversionService;
     *
     * // 方式 2: 直接注入 DynamicConversionService 类型
     * @Resource
     * private DynamicConversionService conversionService;
     * }</pre>
     * </p>
     *
     * @param modelMapper ModelMapper 实例
     * @return DynamicConversionService 实例
     */
    @Bean("modelMapperConversionService")
    @ConditionalOnMissingBean(name = "modelMapperConversionService")
    public ModelMapperConversionService modelMapperConversionService(ModelMapper modelMapper) {
        return new ModelMapperConversionService(modelMapper);
    }

    /**
     * 创建可配置的 ConversionService，用于注册自定义转换器
     * <p>
     * 如果容器中没有 ConfigurableConversionService，则创建一个 DefaultConversionService
     * </p>
     *
     * @return ConfigurableConversionService 实例
     */
    @Bean
    @ConditionalOnMissingBean(ConfigurableConversionService.class)
    public ConfigurableConversionService configurableConversionService() {
        return new DefaultConversionService();
    }

    /**
     * 创建 ModelMapperConversionServiceConfigurer
     * <p>
     * 用于将 ModelMapperConverter 实现自动注册到 Spring ConversionService
     * </p>
     *
     * @param modelMapper ModelMapper 实例
     * @param conversionService ConversionService 实例
     * @param convertersProvider ModelMapperConverter 提供者
     * @return ModelMapperConversionServiceConfigurer 实例
     */
    @Bean
    public ModelMapperConversionServiceConfigurer modelMapperConversionServiceConfigurer(
            ModelMapper modelMapper,
            ConfigurableConversionService conversionService,
            ObjectProvider<List<ModelMapperConverter<?, ?>>> convertersProvider) {
        return new ModelMapperConversionServiceConfigurer(modelMapper, conversionService, convertersProvider);
    }

    private void configureMatchingStrategy(ModelMapper modelMapper, ModelMapperProperties properties) {
        switch (properties.getMatchingStrategy()) {
            case STRICT:
                modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
                break;
            case LOOSE:
                modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
                break;
            case STANDARD:
            default:
                modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STANDARD);
                break;
        }
    }

    /**
     * ConversionService 配置器
     * <p>
     * 负责将 ModelMapperConverter 实现注册到 Spring ConversionService
     * </p>
     */
    public static class ModelMapperConversionServiceConfigurer {

        private final ModelMapper modelMapper;
        private final ConfigurableConversionService conversionService;

        public ModelMapperConversionServiceConfigurer(
                ModelMapper modelMapper,
                ConfigurableConversionService conversionService,
                ObjectProvider<List<ModelMapperConverter<?, ?>>> convertersProvider) {
            this.modelMapper = modelMapper;
            this.conversionService = conversionService;

            // 注册所有 ModelMapperConverter 到 ConversionService
            List<ModelMapperConverter<?, ?>> converters = convertersProvider.getIfAvailable();
            if (converters != null) {
                for (Converter<?, ?> converter : converters) {
                    conversionService.addConverter(converter);
                }
            }
        }

        /**
         * 获取 ModelMapper 实例
         *
         * @return ModelMapper 实例
         */
        public ModelMapper getModelMapper() {
            return modelMapper;
        }

        /**
         * 获取 ConversionService 实例
         *
         * @return ConfigurableConversionService 实例
         */
        public ConfigurableConversionService getConversionService() {
            return conversionService;
        }

        /**
         * 手动注册一个转换器到 ConversionService
         *
         * @param converter 转换器
         */
        public void addConverter(Converter<?, ?> converter) {
            conversionService.addConverter(converter);
        }

        /**
         * 创建并注册一个 ModelMapperConverter
         *
         * @param sourceType 源类型
         * @param targetType 目标类型
         * @param <S> 源类型
         * @param <T> 目标类型
         * @return 创建的转换器
         */
        public <S, T> ModelMapperConverter<S, T> createAndRegisterConverter(Class<S> sourceType, Class<T> targetType) {
            ModelMapperConverter<S, T> converter = new ModelMapperConverter<>(modelMapper, targetType);
            conversionService.addConverter(sourceType, targetType, converter);
            return converter;
        }
    }
}
