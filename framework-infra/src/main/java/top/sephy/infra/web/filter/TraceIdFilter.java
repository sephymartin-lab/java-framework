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

import org.apache.commons.lang3.StringUtils;
import org.apache.skywalking.apm.agent.core.context.ids.GlobalIdGenerator;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.sephy.infra.consts.GlobalSystemConstants;
import top.sephy.infra.utils.ThreadContextUtils;

/**
 * 确保此过滤器在最前面
 */
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String tid = null;
        try {
            tid = request.getHeader(GlobalSystemConstants.TRACE_ID_NAME);
            if (StringUtils.isBlank(tid)) {
                tid = GlobalIdGenerator.generate();
            }
            ThreadContextUtils.setTraceId(tid);
            MDC.put(GlobalSystemConstants.TRACE_ID_NAME, tid);
        } finally {
            // 在响应头中添加 traceId
            if (StringUtils.isNoneBlank(tid)) {
                response.setHeader(GlobalSystemConstants.TRACE_ID_NAME, tid);
            }
            filterChain.doFilter(request, response);
            MDC.remove(GlobalSystemConstants.TRACE_ID_NAME);
        }
    }
}
