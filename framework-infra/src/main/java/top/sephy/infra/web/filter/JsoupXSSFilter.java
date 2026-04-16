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
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.sephy.infra.jackson.XSSRequestWrapper;
import top.sephy.infra.utils.XSSUtils;

@Slf4j
public class JsoupXSSFilter extends OncePerRequestFilter {

    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    private Set<String> excludeUrlPatterns = new TreeSet<>();

    private PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String uri = urlPathHelper.getLookupPathForRequest(request);

        for (String excludeUrlPattern : excludeUrlPatterns) {
            if (pathMatcher.match(excludeUrlPattern, uri)) {
                log.info("{} 匹配 xss 过滤排除路径 {}", uri, excludeUrlPattern);
                filterChain.doFilter(request, response);
                return;
            }
        }

        CachedBodyHttpServletRequest wrapper = null;

        if (request instanceof CachedBodyHttpServletRequest) {
            wrapper = (CachedBodyHttpServletRequest)request;
        } else {
            wrapper = new CachedBodyHttpServletRequest(request);
        }

        String body = StreamUtils.copyToString(wrapper.getInputStream(), StandardCharsets.UTF_8);
        if (!StringUtils.hasText(body)) {
            body = XSSUtils.stripXSS(body);
            wrapper.resetInputStream(body.getBytes());
        }

        XSSRequestWrapper wrappedRequest = new XSSRequestWrapper(wrapper);
        filterChain.doFilter(wrappedRequest, response);
    }
}
