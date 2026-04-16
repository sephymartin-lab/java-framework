/*
 * Copyright 2022-2025 sephy.top
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
/**
 * 日志关键字注解包
 * <p>
 * 提供 {@link top.sephy.infra.logging.annotation.LogKeyword} 注解，
 * 用于通过 SpEL 表达式从方法参数中提取字段信息并放入 SLF4J MDC 中。
 * <p>
 * 使用示例：
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;Service
 *     public class UserService {
 * 
 *         // 单参数场景
 *         &#64;LogKeyword(keyword = "userId", spel = "#{#user.id}")
 *         public void processUser(User user) {
 *             // 方法执行时，user.id 的值会被放入 MDC，key 为 "userId"
 *             log.info("处理用户"); // 日志中会包含 userId
 *         }
 * 
 *         // 多个关键字
 *         &#64;LogKeyword(keyword = "userId", spel = "#{#user.id}")
 *         &#64;LogKeyword(keyword = "userName", spel = "#{#user.name}")
 *         public void processUserWithMultipleKeywords(User user) {
 *             // 方法执行时，两个值都会被放入 MDC
 *             log.info("处理用户"); // 日志中会包含 userId 和 userName
 *         }
 * 
 *         // 多参数场景 - 直接引用参数
 *         &#64;LogKeyword(keyword = "userId", spel = "#{#userId}")
 *         &#64;LogKeyword(keyword = "orderId", spel = "#{#orderId}")
 *         &#64;LogKeyword(keyword = "action", spel = "#{#action}")
 *         public void processOrderAction(Long userId, Long orderId, String action) {
 *             // 可以直接引用方法参数名
 *             log.info("处理订单操作"); // 日志中会包含 userId, orderId, action
 *         }
 * 
 *         // 多参数场景 - 从不同对象提取
 *         &#64;LogKeyword(keyword = "userId", spel = "#{#user.id}")
 *         &#64;LogKeyword(keyword = "orderId", spel = "#{#order.id}")
 *         &#64;LogKeyword(keyword = "orderStatus", spel = "#{#order.status}")
 *         public void updateOrder(User user, Order order) {
 *             // 可以从不同参数对象中提取字段
 *             log.info("更新订单"); // 日志中会包含 userId, orderId, orderStatus
 *         }
 * 
 *         // 复杂表达式
 *         &#64;LogKeyword(keyword = "userInfo", spel = "#{#user.id + ':' + #user.name}")
 *         public void processUserWithComplexSpel(User user) {
 *             // 支持 SpEL 的所有表达式功能
 *             log.info("处理用户"); // 日志中会包含组合后的 userInfo
 *         }
 * 
 *         // 不自动清理
 *         @LogKeyword(keyword = "userId", spel = "#{#user.id}", clearBeforeReturn = false)
 *         public void processUserWithoutClear(User user) {
 *             // 方法执行完成后，MDC 中的 userId 不会被清理
 *             // 需要手动清理或由其他机制清理
 *         }
 *     }
 * }
 * </pre>
 * <p>
 * 配置示例：
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;Configuration
 *     &#64;EnableAspectJAutoProxy
 *     public class LoggingConfig {
 * 
 *         @Bean
 *         public LogKeywordAspect logKeywordAspect() {
 *             return new LogKeywordAspect();
 *         }
 *     }
 * }
 * </pre>
 */
package top.sephy.infra.logging.annotation;
