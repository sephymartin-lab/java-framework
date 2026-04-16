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
package top.sephy.infra.mybatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import top.sephy.infra.enums.BaseEnum;

/**
 * 通用枚举类型处理器
 * <p>
 * 通过反射调用枚举的 fromCode 方法，支持所有实现 BaseEnum 接口的枚举。
 * 这样只需要一个 TypeHandler 就能处理所有枚举，无需为每个枚举创建单独的 TypeHandler。
 * <p>
 * 向后兼容处理：
 * <ul>
 *   <li>优先通过反射调用枚举类的静态方法 fromCode(String code)</li>
 *   <li>如果枚举没有 fromCode 方法，尝试通过 getCode() 匹配</li>
 *   <li>如果匹配失败，尝试查找 UNKNOWN 值</li>
 *   <li>如果都没有，抛出异常（包含详细错误信息）</li>
 * </ul>
 *
 * @param <E> 枚举类型，必须实现 BaseEnum 接口
 * @author sephy
 */
@MappedTypes({})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class BaseEnumTypeHandler<E extends Enum<E> & BaseEnum<E>> extends BaseTypeHandler<E> {

    private final Class<E> type;

    public BaseEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String code = rs.getString(columnName);
        return code == null ? null : fromCode(code);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String code = rs.getString(columnIndex);
        return code == null ? null : fromCode(code);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String code = cs.getString(columnIndex);
        return code == null ? null : fromCode(code);
    }

    /**
     * 通过反射调用枚举的 fromCode 方法
     *
     * @param code 数据库存储值
     * @return 枚举实例
     */
    @SuppressWarnings("unchecked")
    private E fromCode(String code) {
        try {
            // 尝试调用枚举类的静态方法 fromCode(String code)
            java.lang.reflect.Method fromCodeMethod = type.getDeclaredMethod("fromCode", String.class);
            return (E)fromCodeMethod.invoke(null, code);
        } catch (NoSuchMethodException e) {
            // 如果枚举类没有 fromCode 方法，尝试通过 getCode() 匹配
            for (E enumConstant : type.getEnumConstants()) {
                if (enumConstant.getCode().equals(code)) {
                    return enumConstant;
                }
            }
            // 查找 UNKNOWN 值
            try {
                return Enum.valueOf(type, "UNKNOWN");
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                    "未知的枚举值: " + code + " (枚举类: " + type.getName() + ")，且未找到 UNKNOWN 占位符", ex);
            }
        } catch (Exception e) {
            // 如果反射调用失败，尝试通过 getCode() 匹配
            for (E enumConstant : type.getEnumConstants()) {
                if (enumConstant.getCode().equals(code)) {
                    return enumConstant;
                }
            }
            // 查找 UNKNOWN 值
            try {
                return Enum.valueOf(type, "UNKNOWN");
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                    "未知的枚举值: " + code + " (枚举类: " + type.getName() + ")，且未找到 UNKNOWN 占位符。原始异常: " + e.getMessage(),
                    ex);
            }
        }
    }
}
