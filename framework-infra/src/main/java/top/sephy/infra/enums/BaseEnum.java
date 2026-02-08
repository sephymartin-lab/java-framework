/* Copyright 2022-2026 sephy.top
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
 * limitations under the License. */
package top.sephy.infra.enums;

/**
 * 枚举基接口
 * <p>
 * 所有需要支持向后兼容的枚举都应实现此接口，提供 getCode() 方法。
 * 通用 TypeHandler 会通过反射调用枚举的 fromCode(String) 方法进行转换。
 * <p>
 * 实现规范：
 * <ul>
 *   <li>枚举类必须实现此接口</li>
 *   <li>枚举类必须提供静态方法 {@code public static EnumType fromCode(String code)}</li>
 *   <li>fromCode 方法应该防御性处理未知值，返回 UNKNOWN 而不是抛异常</li>
 *   <li>建议添加 UNKNOWN 占位符用于向后兼容</li>
 * </ul>
 *
 * @param <T> 枚举类型
 * @author sephy
 */
public interface BaseEnum<T extends Enum<T>> {

    /**
     * 获取数据库存储值（code）
     *
     * @return 数据库存储值
     */
    String getCode();
}
