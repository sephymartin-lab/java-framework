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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LogKeywordAspect 单元测试
 */
public class LogKeywordAspectTest {

    private TestService testService;

    @BeforeEach
    public void setUp() {
        // 创建目标对象
        TestService target = new TestService();

        // 创建代理工厂
        AspectJProxyFactory factory = new AspectJProxyFactory(target);

        // 添加切面
        LogKeywordAspect aspect = new LogKeywordAspect();
        factory.addAspect(aspect);

        // 获取代理对象
        testService = factory.getProxy();
    }

    @AfterEach
    public void tearDown() {
        // 清理 MDC
        MDC.clear();
    }

    @Test
    public void testSingleLogKeyword() {
        // Given
        User user = new User(1L, "张三");

        // When
        testService.processUser(user);

        // Then - MDC 应该已被清理
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    public void testMultipleLogKeywords() {
        // Given
        User user = new User(2L, "李四");

        // When
        testService.processUserWithMultipleKeywords(user);

        // Then - MDC 应该已被清理
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("userName")).isNull();
    }

    @Test
    public void testClearBeforeReturnFalse() {
        // Given
        User user = new User(3L, "王五");

        // When
        testService.processUserWithoutClear(user);

        // Then - MDC 不应该被清理
        assertThat(MDC.get("userId")).isEqualTo("3");

        // 手动清理
        MDC.remove("userId");
    }

    @Test
    public void testNullValue() {
        // Given
        User user = new User(4L, null);

        // When
        testService.processUser(user);

        // Then - MDC 应该已被清理
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    public void testComplexSpelExpression() {
        // Given
        Order order = new Order(100L, new User(5L, "赵六"), "已完成");

        // When
        testService.processOrder(order);

        // Then - MDC 应该已被清理
        assertThat(MDC.get("orderId")).isNull();
        assertThat(MDC.get("orderUserId")).isNull();
    }

    @Test
    public void testMethodWithException() {
        // Given
        User user = new User(6L, "异常用户");

        // When & Then
        assertThatThrownBy(() -> testService.processUserWithException(user)).isInstanceOf(RuntimeException.class)
            .hasMessage("测试异常");

        // MDC 应该已被清理（即使方法抛出异常）
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    public void testInvalidSpelExpression() {
        // Given
        User user = new User(7L, "测试用户");

        // When - 即使 SpEL 表达式无效，方法也应该正常执行
        String result = testService.processUserWithInvalidSpel(user);

        // Then
        assertThat(result).isEqualTo("处理完成");
        // MDC 不应该包含无效的 key（因为解析失败）
        assertThat(MDC.get("invalidKey")).isNull();
    }

    @Test
    public void testMultipleParameters() {
        // Given
        Long userId = 100L;
        Long orderId = 200L;
        String action = "CREATE";

        // When
        testService.processOrderAction(userId, orderId, action);

        // Then - MDC 应该已被清理
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("orderId")).isNull();
        assertThat(MDC.get("action")).isNull();
    }

    @Test
    public void testMultipleParametersWithObjects() {
        // Given
        User user = new User(10L, "多参数用户");
        Order order = new Order(20L, user, "PENDING");

        // When
        testService.updateOrder(user, order);

        // Then - MDC 应该已被清理
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("orderId")).isNull();
        assertThat(MDC.get("orderStatus")).isNull();
    }

    @Test
    public void testComplexSpelExpressions() {
        // Given
        User user = new User(15L, "复杂表达式用户");

        // When
        testService.processUserWithComplexSpel(user);

        // Then - MDC 应该已被清理
        assertThat(MDC.get("userInfo")).isNull();
    }

    /**
     * 测试服务类
     */
    public static class TestService {

        @LogKeyword(keyword = "userId", spel = "#{#user.id}")
        public void processUser(User user) {
            // 在方法执行期间，MDC 应该包含 userId
            assertThat(MDC.get("userId")).isEqualTo(String.valueOf(user.getId()));
        }

        @LogKeyword(keyword = "userId", spel = "#{#user.id}")
        @LogKeyword(keyword = "userName", spel = "#{#user.name}")
        public void processUserWithMultipleKeywords(User user) {
            // 在方法执行期间，MDC 应该包含两个 key
            assertThat(MDC.get("userId")).isEqualTo(String.valueOf(user.getId()));
            assertThat(MDC.get("userName")).isEqualTo(user.getName());
        }

        @LogKeyword(keyword = "userId", spel = "#{#user.id}", clearBeforeReturn = false)
        public void processUserWithoutClear(User user) {
            // 在方法执行期间，MDC 应该包含 userId
            assertThat(MDC.get("userId")).isEqualTo(String.valueOf(user.getId()));
        }

        @LogKeyword(keyword = "orderId", spel = "#{#order.id}")
        @LogKeyword(keyword = "orderUserId", spel = "#{#order.user.id}")
        public void processOrder(Order order) {
            // 在方法执行期间，MDC 应该包含两个 key
            assertThat(MDC.get("orderId")).isEqualTo(String.valueOf(order.getId()));
            assertThat(MDC.get("orderUserId")).isEqualTo(String.valueOf(order.getUser().getId()));
        }

        @LogKeyword(keyword = "userId", spel = "#{#user.id}")
        public void processUserWithException(User user) {
            assertThat(MDC.get("userId")).isEqualTo(String.valueOf(user.getId()));
            throw new RuntimeException("测试异常");
        }

        @LogKeyword(keyword = "invalidKey", spel = "#{#user.nonExistentField}")
        public String processUserWithInvalidSpel(User user) {
            return "处理完成";
        }

        @LogKeyword(keyword = "userId", spel = "#{#userId}")
        @LogKeyword(keyword = "orderId", spel = "#{#orderId}")
        @LogKeyword(keyword = "action", spel = "#{#action}")
        public void processOrderAction(Long userId, Long orderId, String action) {
            // 在方法执行期间，MDC 应该包含三个参数
            assertThat(MDC.get("userId")).isEqualTo(String.valueOf(userId));
            assertThat(MDC.get("orderId")).isEqualTo(String.valueOf(orderId));
            assertThat(MDC.get("action")).isEqualTo(action);
        }

        @LogKeyword(keyword = "userId", spel = "#{#user.id}")
        @LogKeyword(keyword = "orderId", spel = "#{#order.id}")
        @LogKeyword(keyword = "orderStatus", spel = "#{#order.status}")
        public void updateOrder(User user, Order order) {
            // 在方法执行期间，MDC 应该包含来自不同参数对象的字段
            assertThat(MDC.get("userId")).isEqualTo(String.valueOf(user.getId()));
            assertThat(MDC.get("orderId")).isEqualTo(String.valueOf(order.getId()));
            assertThat(MDC.get("orderStatus")).isEqualTo(order.getStatus());
        }

        @LogKeyword(keyword = "userInfo", spel = "#{#user.id + ':' + #user.name}")
        public void processUserWithComplexSpel(User user) {
            // 在方法执行期间，MDC 应该包含组合后的值
            String expected = user.getId() + ":" + user.getName();
            assertThat(MDC.get("userInfo")).isEqualTo(expected);
        }
    }

    /**
     * 测试用户类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {

        private Long id;

        private String name;
    }

    /**
     * 测试订单类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {

        private Long id;

        private User user;

        private String status;
    }
}
