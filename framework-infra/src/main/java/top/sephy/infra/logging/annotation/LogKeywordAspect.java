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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;

import lombok.extern.slf4j.Slf4j;
import top.sephy.infra.consts.AopOrderConstants;
import top.sephy.infra.utils.SpELUtils;

/**
 * LogKeyword 注解的切面处理类
 * <p>
 * 负责拦截带有 {@link LogKeyword} 或 {@link LogKeywords} 注解的方法，
 * 通过 SpEL 表达式从方法参数中提取值并放入 SLF4J MDC 中
 * <p>
 * <b>AOP 执行顺序说明：</b>
 * <ul>
 * <li>Order 值越小，优先级越高</li>
 * <li>默认 Order = Integer.MAX_VALUE（最低优先级）</li>
 * <li>建议的顺序：日志追踪(100) > 分布式锁(200) > 事务(300) > 业务逻辑</li>
 * </ul>
 * <p>
 * 当前优先级：{@link AopOrderConstants#LOG_KEYWORD}（较高优先级，确保在其他切面之前设置 MDC）
 * 
 * @see org.springframework.core.annotation.Order
 * @see AopOrderConstants
 */
@Slf4j
@Aspect
@Order(AopOrderConstants.LOG_KEYWORD)
public class LogKeywordAspect {

    /**
     * 匹配带有 @LogKeyword 或 @LogKeywords 注解的方法
     * <p>
     * 注意：这里使用完全限定类名（FQCN）是 AspectJ 的标准做法，
     * 它是通过类型引用而非字符串路径，IDE 支持重构和跳转，
     * 编译时会进行类型检查，因此是类型安全的。
     */
    @Pointcut("@annotation(top.sephy.infra.logging.annotation.LogKeyword) || "
        + "@annotation(top.sephy.infra.logging.annotation.LogKeywords)")
    public void logKeywordPointcut() {}

    @Around("logKeywordPointcut()")
    public Object processLogKeyword(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取方法上的所有 LogKeyword 注解
        LogKeyword[] logKeywords = getLogKeywords(method);

        // 存储需要清理的 MDC key
        List<String> keysToClean = new ArrayList<>();

        try {
            // 处理每个 LogKeyword 注解
            for (LogKeyword logKeyword : logKeywords) {
                try {
                    String keyword = logKeyword.keyword();
                    String spel = logKeyword.spel();
                    boolean clearBeforeReturn = logKeyword.clearBeforeReturn();

                    // 解析 SpEL 表达式
                    Object value = SpELUtils.parseToObject(spel, joinPoint);

                    // 将值放入 MDC
                    if (value != null) {
                        MDC.put(keyword, String.valueOf(value));
                        log.trace("已将关键字 [{}] 的值 [{}] 放入 MDC", keyword, value);
                    } else {
                        MDC.put(keyword, "");
                        log.trace("已将关键字 [{}] 的空值放入 MDC", keyword);
                    }

                    // 如果需要清理，记录 key
                    if (clearBeforeReturn) {
                        keysToClean.add(keyword);
                    }

                } catch (Exception e) {
                    log.warn("处理 LogKeyword 注解时发生异常，keyword: {}, spel: {}", logKeyword.keyword(), logKeyword.spel(), e);
                }
            }

            // 执行目标方法
            return joinPoint.proceed();

        } finally {
            // 清理 MDC
            for (String key : keysToClean) {
                MDC.remove(key);
                log.trace("已从 MDC 中移除关键字 [{}]", key);
            }
        }
    }

    /**
     * 获取方法上的所有 LogKeyword 注解
     * 
     * @param method 方法对象
     * @return LogKeyword 注解数组
     */
    private LogKeyword[] getLogKeywords(Method method) {
        // 首先尝试获取 LogKeywords 容器注解
        LogKeywords logKeywords = method.getAnnotation(LogKeywords.class);
        if (logKeywords != null) {
            return logKeywords.value();
        }

        // 如果没有容器注解，尝试获取单个 LogKeyword 注解
        LogKeyword logKeyword = method.getAnnotation(LogKeyword.class);
        if (logKeyword != null) {
            return new LogKeyword[] {logKeyword};
        }

        // 如果都没有，返回空数组
        return new LogKeyword[0];
    }
}
