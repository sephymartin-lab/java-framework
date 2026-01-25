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

import org.modelmapper.ModelMapper;
import org.springframework.core.convert.converter.Converter;

/**
 * 基于 ModelMapper 的通用 Spring Converter 实现
 * <p>
 * 通过继承此类，可以将 ModelMapper 的映射能力与 Spring ConversionService 集成。
 * 使用时需要创建具体的子类或实例，指定源类型和目标类型。
 * </p>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * @Component
 * public class UserDOToUserDTOConverter extends ModelMapperConverter<UserDO, UserDTO> {
 *     public UserDOToUserDTOConverter(ModelMapper modelMapper) {
 *         super(modelMapper, UserDTO.class);
 *     }
 * }
 * }</pre>
 *
 * @param <S> 源类型
 * @param <T> 目标类型
 * @author sephy
 */
public class ModelMapperConverter<S, T> implements Converter<S, T> {

    private final ModelMapper modelMapper;
    private final Class<T> targetType;

    /**
     * 构造函数
     *
     * @param modelMapper ModelMapper 实例
     * @param targetType 目标类型
     */
    public ModelMapperConverter(ModelMapper modelMapper, Class<T> targetType) {
        this.modelMapper = modelMapper;
        this.targetType = targetType;
    }

    @Override
    public T convert(S source) {
        if (source == null) {
            return null;
        }
        return modelMapper.map(source, targetType);
    }

    /**
     * 获取目标类型
     *
     * @return 目标类型 Class
     */
    public Class<T> getTargetType() {
        return targetType;
    }

    /**
     * 获取 ModelMapper 实例
     *
     * @return ModelMapper 实例
     */
    protected ModelMapper getModelMapper() {
        return modelMapper;
    }
}
