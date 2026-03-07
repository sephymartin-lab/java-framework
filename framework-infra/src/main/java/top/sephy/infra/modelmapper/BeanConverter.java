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

import java.util.Collection;
import java.util.List;

/**
 * 通用对象转换接口
 * <p>
 * 提供单对象和集合的转换能力，以及属性复制功能
 * </p>
 *
 * @author sephy
 */
public interface BeanConverter {

    /**
     * 将源对象转换为目标类型
     *
     * @param source 源对象
     * @param targetType 目标类型
     * @param <T> 目标类型泛型
     * @return 转换后的目标对象，如果源对象为 null 则返回 null
     */
    <T> T convert(Object source, Class<T> targetType);

    /**
     * 将源对象集合转换为目标类型集合
     *
     * @param sources 源对象集合
     * @param targetType 目标类型
     * @param <T> 目标类型泛型
     * @return 转换后的目标对象列表，如果源集合为 null 或空则返回空列表
     */
    <T> List<T> convertList(Collection<?> sources, Class<T> targetType);

    /**
     * 将源对象的属性复制到目标对象
     * <p>
     * 用于更新已存在的对象，而不是创建新对象
     * </p>
     *
     * @param source 源对象
     * @param destination 目标对象
     */
    void copyProperties(Object source, Object destination);
}
