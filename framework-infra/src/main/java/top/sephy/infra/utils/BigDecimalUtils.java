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
package top.sephy.infra.utils;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * BigDecimal 工具类
 * <p>
 * 提供安全的 BigDecimal 运算方法，自动处理 null 值（将 null 视为 0）
 * </p>
 *
 * @author sephy
 */
public abstract class BigDecimalUtils {

    public static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * 将 null 值转换为 BigDecimal.ZERO
     *
     * @param value 待转换的值
     * @return 如果 value 为 null 则返回 BigDecimal.ZERO，否则返回原值
     */
    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 加法运算（两个参数）
     * <p>
     * 如果参数为 null，则视为 0 参与运算
     * </p>
     *
     * @param a 第一个加数
     * @param b 第二个加数
     * @return a + b 的结果，不会返回 null
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return nullToZero(a).add(nullToZero(b));
    }

    /**
     * 加法运算（可变参数）
     * <p>
     * 如果参数为 null，则视为 0 参与运算
     * </p>
     *
     * @param values 待相加的值数组
     * @return 所有值相加的结果，不会返回 null。如果 values 为 null 或空数组，返回 BigDecimal.ZERO
     */
    public static BigDecimal add(BigDecimal... values) {
        if (values == null || values.length == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            result = result.add(nullToZero(value));
        }
        return result;
    }

    /**
     * 加法运算（两个参数，带精度控制）
     * <p>
     * 如果参数为 null，则视为 0 参与运算
     * </p>
     *
     * @param mc 数学上下文，用于控制精度和舍入模式
     * @param a 第一个加数
     * @param b 第二个加数
     * @return a + b 的结果，不会返回 null
     */
    public static BigDecimal add(MathContext mc, BigDecimal a, BigDecimal b) {
        return nullToZero(a).add(nullToZero(b), mc);
    }

    /**
     * 加法运算（可变参数，带精度控制）
     * <p>
     * 如果参数为 null，则视为 0 参与运算
     * </p>
     *
     * @param mc 数学上下文，用于控制精度和舍入模式
     * @param values 待相加的值数组
     * @return 所有值相加的结果，不会返回 null。如果 values 为 null 或空数组，返回 BigDecimal.ZERO
     */
    public static BigDecimal add(MathContext mc, BigDecimal... values) {
        if (values == null || values.length == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            result = result.add(nullToZero(value), mc);
        }
        return result;
    }

    /**
     * 减法运算（两个参数）
     * <p>
     * 如果参数为 null，则视为 0 参与运算
     * </p>
     *
     * @param a 被减数
     * @param b 减数
     * @return a - b 的结果，不会返回 null
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return nullToZero(a).subtract(nullToZero(b));
    }

    /**
     * 减法运算（两个参数，带精度控制）
     * <p>
     * 如果参数为 null，则视为 0 参与运算
     * </p>
     *
     * @param mc 数学上下文，用于控制精度和舍入模式
     * @param a 被减数
     * @param b 减数
     * @return a - b 的结果，不会返回 null
     */
    public static BigDecimal subtract(MathContext mc, BigDecimal a, BigDecimal b) {
        return nullToZero(a).subtract(nullToZero(b), mc);
    }
}
