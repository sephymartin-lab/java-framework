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
package top.sephy.infra.consts;

/**
 * AOP 切面执行顺序常量
 * <p>
 * 定义系统中各个切面的执行顺序，Order 值越小，优先级越高
 * <p>
 * <b>执行顺序规则：</b>
 * <ul>
 * <li>Order 值小的切面最先进入，最后退出（洋葱模型）</li>
 * <li>默认 Order = Integer.MAX_VALUE（最低优先级）</li>
 * <li>相同 Order 值时，执行顺序不确定</li>
 * </ul>
 * <p>
 * <b>推荐的顺序分配：</b>
 * <ul>
 * <li>1-50: 基础设施层（请求追踪、性能监控）</li>
 * <li>100: 日志追踪层（MDC 设置）</li>
 * <li>200: 分布式锁层</li>
 * <li>300: 事务管理层</li>
 * <li>400: 缓存层</li>
 * <li>500: 权限校验层</li>
 * <li>1000+: 业务切面层</li>
 * </ul>
 * 
 * @see org.springframework.core.annotation.Order
 * @see org.springframework.core.Ordered
 */
public final class AopOrderConstants {

    private AopOrderConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    // ==================== 基础设施层 (1-50) ====================

    /**
     * 性能监控切面
     * <p>
     * 最高优先级，用于监控整个方法的执行时间（包括所有切面）
     */
    public static final int PERFORMANCE_MONITOR = 50;

    // ==================== 日志追踪层 (100) ====================

    /**
     * 日志关键字切面 {@link top.sephy.infra.logging.annotation.LogKeywordAspect}
     * <p>
     * 设置 MDC 上下文，确保后续所有切面的日志都包含追踪信息
     */
    public static final int LOG_KEYWORD = 100;

    // ==================== 分布式锁层 (200) ====================

    /**
     * Redis 分布式锁切面 {@link top.sephy.infra.lock.annotation.RedissonLockByAspect}
     * <p>
     * 在事务之前获取锁，避免死锁和长时间持有数据库连接
     */
    public static final int DISTRIBUTED_LOCK = 200;

    // ==================== 事务管理层 (300) ====================

    /**
     * 数据库事务切面
     * <p>
     * Spring @Transactional 默认优先级较低，建议在锁内部开启事务
     */
    public static final int TRANSACTION = 300;

    // ==================== 缓存层 (400) ====================

    /**
     * 缓存切面
     * <p>
     * Spring @Cacheable 等缓存注解
     */
    public static final int CACHE = 400;

    // ==================== 权限校验层 (500) ====================

    /**
     * 权限校验切面
     * <p>
     * Spring Security @PreAuthorize 等权限注解
     */
    public static final int SECURITY = 500;

    // ==================== 业务切面层 (1000+) ====================

    /**
     * 业务审计切面
     * <p>
     * 业务相关的审计日志，在所有基础设施切面之后执行
     */
    public static final int BUSINESS_AUDIT = 1000;

    /**
     * 业务日志切面
     * <p>
     * 业务相关的日志记录
     */
    public static final int BUSINESS_LOGGING = 1100;

    /**
     * 业务监控切面
     * <p>
     * 业务指标监控和统计
     */
    public static final int BUSINESS_MONITOR = 1200;
}
