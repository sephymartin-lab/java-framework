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
package top.sephy.infra.web.filter;

import java.io.IOException;
import java.util.Collection;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.Setter;

/**
 * @description: 用于排除指定路径的过滤器
 */
public class ExcludePatternFilterWrapper extends OncePerRequestFilter implements Ordered {

    @Setter
    private int order = 0;

    private PathMatcher pathMatcher = new AntPathMatcher();

    private Filter delegate;

    private Collection<String> excludePatterns;

    public ExcludePatternFilterWrapper(@NonNull Filter delegate, @NonNull Collection<String> excludePatterns) {
        this.delegate = delegate;
        this.excludePatterns = excludePatterns;
        if (delegate instanceof Ordered) {
            this.order = ((Ordered)delegate).getOrder();
        }
        Order annotation = AnnotationUtils.getAnnotation(delegate.getClass(), Order.class);
        if (annotation != null) {
            this.order = annotation.value();
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (excludePatterns.stream().noneMatch(pattern -> pathMatcher.match(pattern, request.getServletPath()))) {
            delegate.doFilter(request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    @Override
    public int getOrder() {
        return order;
    }
}
