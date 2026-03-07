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
package top.sephy.infra.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 日志关键字注解，用于通过 SpEL 表达式从方法参数中提取字段信息并放入 SLF4J MDC 中
 * <p>
 * <b>单参数场景：</b>
 * 
 * <pre>
 * &#64;LogKeyword(keyword = "userId", spel = "#{#user.id}")
 * public void processUser(User user) {
 *     // 方法执行时，user.id 的值会被放入 MDC，key 为 "userId"
 * }
 * </pre>
 * <p>
 * <b>多个关键字：</b>
 * 
 * <pre>
 * &#64;LogKeyword(keyword = "userId", spel = "#{#user.id}")
 * &#64;LogKeyword(keyword = "userName", spel = "#{#user.name}")
 * public void processUser(User user) {
 *     // 方法执行时，两个值都会被放入 MDC
 * }
 * </pre>
 * <p>
 * <b>多参数场景：</b>
 * 
 * <pre>
 * &#64;LogKeyword(keyword = "userId", spel = "#{#userId}")
 * &#64;LogKeyword(keyword = "orderId", spel = "#{#orderId}")
 * &#64;LogKeyword(keyword = "action", spel = "#{#action}")
 * public void processOrder(Long userId, Long orderId, String action) {
 *     // 可以直接引用方法参数名
 * }
 * </pre>
 * <p>
 * <b>多参数 + 对象属性：</b>
 * 
 * <pre>
 * &#64;LogKeyword(keyword = "userId", spel = "#{#user.id}")
 * &#64;LogKeyword(keyword = "orderId", spel = "#{#order.id}")
 * &#64;LogKeyword(keyword = "orderStatus", spel = "#{#order.status}")
 * public void updateOrder(User user, Order order) {
 *     // 可以从不同参数对象中提取字段
 * }
 * </pre>
 * <p>
 * <b>复杂表达式：</b>
 * 
 * <pre>
 * &#64;LogKeyword(keyword = "userInfo", spel = "#{#user.id + ':' + #user.name}")
 * &#64;LogKeyword(keyword = "hasPermission", spel = "#{#user.roles.contains('ADMIN')}")
 * public void processUser(User user) {
 *     // 支持 SpEL 的所有表达式功能
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(LogKeywords.class)
public @interface LogKeyword {

    /**
     * MDC 中的 key 名称
     * 
     * @return MDC key
     */
    String keyword();

    /**
     * SpEL 表达式，用于从方法参数中提取值
     * <p>
     * 表达式中可以使用 #{#paramName} 来引用方法参数
     * 
     * @return SpEL 表达式
     */
    String spel();

    /**
     * 是否在方法返回前清理 MDC 中的值
     * <p>
     * 默认为 true，即方法执行完成后自动清理
     * 
     * @return 是否清理
     */
    boolean clearBeforeReturn() default true;
}
