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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.SneakyThrows;
import top.sephy.infra.utils.JacksonUtils;
import top.sephy.infra.utils.ServletHttpRequestUtils;

/**
 * 保存请求日志
 */
public class CustomRequestLoggingFilter extends AbstractRequestLoggingFilter {

    static int CACHE_LIMIT = 1_048_576;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        boolean isFirstRequest = !isAsyncDispatch(request);
        HttpServletRequest requestToUse = request;

        if (isIncludePayload() && isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
            requestToUse = new CustomContentCachingRequestWrapper(request, getMaxPayloadLength());
        }

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(requestToUse, response);
        } finally {
            boolean shouldLog = shouldLog(requestToUse);
            if (shouldLog && !isAsyncStarted(requestToUse)) {
                long endTime = System.currentTimeMillis();
                long timeTaken = endTime - startTime;
                Map<String, Object> message = createMessage(requestToUse, response, timeTaken);
                if (logger.isInfoEnabled()) {
                    logger.info(JacksonUtils.toJson(message));
                }
            }
        }
    }

    protected Map<String, Object> createMessage(HttpServletRequest request, HttpServletResponse response,
        long timeTaken) {
        Map<String, Object> msg = new LinkedHashMap<>();
        // msg.put(GlobalSystemConstants.TRACE_ID_NAME, MDC.get(GlobalSystemConstants.TRACE_ID_NAME));
        msg.put("method", request.getMethod());
        msg.put("ip", ServletHttpRequestUtils.getIpAddress(request));
        msg.put("uri", request.getRequestURI());

        if (isIncludeQueryString()) {
            String queryString = request.getQueryString();
            if (queryString != null) {
                msg.put("queryString", queryString);
            }
        }

        if (isIncludeClientInfo()) {
            String client = request.getRemoteAddr();
            if (StringUtils.hasLength(client)) {
                msg.put("client", client);
            }
            HttpSession session = request.getSession(false);
            if (session != null) {
                msg.put("session", session.getId());
            }
            String user = request.getRemoteUser();
            if (user != null) {
                msg.put("user", user);
            }
        }

        if (isIncludeHeaders()) {
            HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
            if (getHeaderPredicate() != null) {
                Enumeration<String> names = request.getHeaderNames();
                while (names.hasMoreElements()) {
                    String header = names.nextElement();
                    if (!getHeaderPredicate().test(header)) {
                        headers.set(header, "masked");
                    }
                }
            }
            msg.put("request-headers", headers.toSingleValueMap());
        }

        if (isIncludePayload()) {
            String payload = getMessagePayload(request);
            if (payload != null) {
                msg.put("payload", payload);
            }
        }

        if (isIncludeHeaders()) {
            HttpHeaders headers = new ServletServerHttpResponse(response).getHeaders();
            if (getHeaderPredicate() != null) {
                Enumeration<String> names = request.getHeaderNames();
                while (names.hasMoreElements()) {
                    String header = names.nextElement();
                    if (!getHeaderPredicate().test(header)) {
                        headers.set(header, "masked");
                    }
                }
            }
            msg.put("response-headers", headers.toSingleValueMap());
        }

        Exception ex = (Exception)request.getAttribute("_exception");
        if (ex != null) {
            HashMap<String, Object> exception = new HashMap<>();
            msg.put("exception", exception);
            exception.put("type", ex.getClass().getName());
            exception.put("message", ex.getMessage());
            exception.put("stack", ExceptionUtils.getStackTrace(ex));
        }

        msg.put("status", response.getStatus());
        msg.put("timeTaken", timeTaken);

        return msg;
    }

    @SneakyThrows
    @Override
    protected String getMessagePayload(HttpServletRequest request) {
        CustomContentCachingRequestWrapper wrapper =
            WebUtils.getNativeRequest(request, CustomContentCachingRequestWrapper.class);
        if (wrapper != null) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length == 0 && wrapper.isJsonPost()) {
                return StreamUtils.copyToString(wrapper.getInputStream(),
                    Charset.forName(wrapper.getCharacterEncoding()));
            }
            if (buf.length > 0) {
                int length = Math.min(buf.length, getMaxPayloadLength());
                try {
                    return new String(buf, 0, length, wrapper.getCharacterEncoding());
                } catch (UnsupportedEncodingException ex) {
                    return "[unknown]";
                }
            }
        }
        return null;
    }

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {

    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }

    public static class CustomContentCachingRequestWrapper extends ContentCachingRequestWrapper {

        // 原子变量，用来区分首次读取还是非首次
        private AtomicBoolean isFirst = new AtomicBoolean(true);

        public CustomContentCachingRequestWrapper(HttpServletRequest request) {
            super(request, CACHE_LIMIT);
        }

        public CustomContentCachingRequestWrapper(HttpServletRequest request, int contentCacheLimit) {
            super(request, contentCacheLimit);
        }

        protected boolean isJsonPost() {
            String contentType = getContentType();
            return (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)
                && HttpMethod.POST.matches(getMethod()));
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {

            if (isFirst.compareAndSet(true, false)) {
                // 首次读取直接调父类的方法，这一次执行完之后 缓存流中有数据了
                return super.getInputStream();
            }

            // 用缓存流构建一个新的输入流
            return new ServletInputStreamNew(super.getContentAsByteArray());
        }

        // 参考自 DelegatingServletInputStream
        class ServletInputStreamNew extends ServletInputStream {

            private InputStream sourceStream;

            private boolean finished = false;

            public ServletInputStreamNew(byte[] bytes) {
                // 构建一个普通的输入流
                this.sourceStream = new ByteArrayInputStream(bytes);
            }

            @Override
            public int read() throws IOException {
                int data = this.sourceStream.read();
                if (data == -1) {
                    this.finished = true;
                }
                return data;
            }

            @Override
            public int available() throws IOException {
                return this.sourceStream.available();
            }

            @Override
            public void close() throws IOException {
                super.close();
                this.sourceStream.close();
            }

            @Override
            public boolean isFinished() {
                return this.finished;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException();
            }
        }
    }
}
