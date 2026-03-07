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
package top.sephy.infra.modelmapper;

import org.modelmapper.ModelMapper;

/**
 * 用于自定义 ModelMapper 映射规则的注册接口
 * <p>
 * 实现此接口并标注 @Component，将自动被扫描并在 ModelMapper 初始化后调用，
 * 允许用户添加自定义的 TypeMap、Converter 等映射规则。
 * </p>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * @Component
 * public class UserMappingRegistrar implements ModelMapperConverterRegistrar {
 *
 *     @Override
 *     public void configure(ModelMapper modelMapper) {
 *         modelMapper.typeMap(UserDO.class, UserDTO.class)
 *             .addMappings(mapper -> {
 *                 mapper.map(UserDO::getUsername, UserDTO::setLoginName);
 *                 mapper.skip(UserDTO::setPassword);
 *             });
 *     }
 * }
 * }</pre>
 *
 * @author sephy
 */
@FunctionalInterface
public interface ModelMapperConverterRegistrar {

    /**
     * 配置 ModelMapper
     * <p>
     * 在此方法中可以添加自定义的 TypeMap、Converter、PropertyMap 等
     * </p>
     *
     * @param modelMapper ModelMapper 实例
     */
    void configure(ModelMapper modelMapper);
}
