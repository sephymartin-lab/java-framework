/* Copyright 2022-2026 sephy.top
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
 * limitations under the License. */
package top.sephy.infra.webclient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;import top.sephy.infra.consts.HttpHeaderConstants;

/**
 * WebClient 请求响应日志记录过滤器 记录所有 WebClient 请求和响应的详细信息
 * 
 * @author sephy
 */
@Slf4j
public class WebClientLoggingFilter implements ExchangeFilterFunction {

    private static final int MAX_BODY_LENGTH = 1000;
    private static final String SENSITIVE_HEADER_AUTHORIZATION = "Authorization";
    private static final String MASKED_VALUE = "***";
    private static final String HEADER_TRACE_ID = HttpHeaderConstants.HEADER_TRACE_ID;
    private static final String MDC_TRACE_ID_KEY = "traceId";

    private final DataBufferFactory bufferFactory;

    public WebClientLoggingFilter() {
        this.bufferFactory = new DefaultDataBufferFactory();
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String traceId = resolveTraceId(request);

        // 记录请求信息
        withTraceId(traceId, () -> logRequest(request));

        // 执行请求并记录响应
        return next.exchange(request).flatMap(response -> {
            // 记录响应头信息
            withTraceId(traceId, () -> logResponseHeaders(request, response));

            // 读取响应体并记录，然后重新包装响应
            MediaType contentType = response.headers().contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
            if (isTextContent(contentType)) {
                // 使用 bodyToFlux 读取所有 DataBuffer
                return response.bodyToFlux(DataBuffer.class).collectList().flatMap(dataBuffers -> {
                    // 合并所有 DataBuffer 并转换为字符串
                    return DataBufferUtils.join(Flux.fromIterable(dataBuffers)).flatMap(buffer -> {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        DataBufferUtils.release(buffer);

                        // 转换为字符串并记录
                        String body = new String(bytes, StandardCharsets.UTF_8);
                        withTraceId(traceId, () -> logResponseBody(request, response, body));

                        // 重新创建响应，使用原始字节数组
                        DataBuffer newBuffer = bufferFactory.wrap(bytes);
                        return Mono.just(ClientResponse.create(response.statusCode())
                            .headers(headers -> headers.addAll(response.headers().asHttpHeaders()))
                            .body(Flux.just(newBuffer)).build());
                    });
                }).onErrorResume(error -> {
                    withTraceId(traceId,
                        () -> log.error("WebClient 响应体读取失败: {} {}", request.method(), request.url(), error));
                    return Mono.just(response);
                });
            } else {
                withTraceId(traceId, () -> log.debug("响应体: [非文本内容，Content-Type: {}]", contentType));
                return Mono.just(response);
            }
        }).onErrorResume(error -> {
            withTraceId(traceId, () -> log.error("WebClient 请求失败: {} {}", request.method(), request.url(), error));
            return Mono.error(error);
        });
    }

    private String resolveTraceId(ClientRequest request) {
        String traceId = request.headers().getFirst(HEADER_TRACE_ID);
        if (StringUtils.isBlank(traceId)) {
            traceId = MDC.get(MDC_TRACE_ID_KEY);
        }
        return StringUtils.trimToNull(traceId);
    }

    private <T> T withTraceId(String traceId, Supplier<T> supplier) {
        if (StringUtils.isBlank(traceId)) {
            return supplier.get();
        }
        String previousTraceId = MDC.get(MDC_TRACE_ID_KEY);
        try {
            MDC.put(MDC_TRACE_ID_KEY, traceId);
            return supplier.get();
        } finally {
            if (StringUtils.isBlank(previousTraceId)) {
                MDC.remove(MDC_TRACE_ID_KEY);
            } else {
                MDC.put(MDC_TRACE_ID_KEY, previousTraceId);
            }
        }
    }

    private void withTraceId(String traceId, Runnable runnable) {
        withTraceId(traceId, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 记录请求信息
     */
    private void logRequest(ClientRequest request) {
        if (!log.isDebugEnabled()) {
            return;
        }

        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("\n========== WebClient 请求 ==========\n");
        logBuilder.append("方法: ").append(request.method()).append("\n");
        logBuilder.append("URL: ").append(request.url()).append("\n");

        // 记录请求头
        HttpHeaders headers = request.headers();
        if (!headers.isEmpty()) {
            logBuilder.append("请求头:\n");
            headers.forEach((name, values) -> {
                String headerValue = maskSensitiveHeader(name, values);
                logBuilder.append("  ").append(name).append(": ").append(headerValue).append("\n");
            });
        }

        // 对于请求体，由于是响应式流，我们只能记录 Content-Type
        // 实际的请求体内容需要在调用方记录
        MediaType contentType = headers.getContentType();
        if (contentType != null) {
            logBuilder.append("Content-Type: ").append(contentType).append("\n");
        }

        logBuilder.append("=====================================");
        log.debug(logBuilder.toString());
    }

    /**
     * 记录响应头信息
     */
    private void logResponseHeaders(ClientRequest request, ClientResponse response) {
        if (!log.isDebugEnabled()) {
            return;
        }

        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("\n========== WebClient 响应 ==========\n");
        logBuilder.append("请求: ").append(request.method()).append(" ").append(request.url()).append("\n");
        logBuilder.append("状态码: ").append(response.statusCode()).append("\n");

        // 记录响应头
        HttpHeaders headers = response.headers().asHttpHeaders();
        if (!headers.isEmpty()) {
            logBuilder.append("响应头:\n");
            headers.forEach((name, values) -> {
                String headerValue = maskSensitiveHeader(name, values);
                logBuilder.append("  ").append(name).append(": ").append(headerValue).append("\n");
            });
        }

        logBuilder.append("=====================================");
        log.debug(logBuilder.toString());
    }

    /**
     * 记录响应体
     */
    private void logResponseBody(ClientRequest request, ClientResponse response, String body) {
        if (!log.isDebugEnabled()) {
            return;
        }

        String bodyPreview = truncateBody(body);
        log.debug("响应体: {}\n=====================================", bodyPreview);
    }

    /**
     * 脱敏敏感请求头
     */
    private String maskSensitiveHeader(String headerName, List<String> values) {
        if (SENSITIVE_HEADER_AUTHORIZATION.equalsIgnoreCase(headerName)) {
            return MASKED_VALUE;
        }
        return values.stream().collect(Collectors.joining(", "));
    }

    /**
     * 判断是否为文本内容
     */
    private boolean isTextContent(MediaType contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.isCompatibleWith(MediaType.APPLICATION_JSON)
            || contentType.isCompatibleWith(MediaType.TEXT_PLAIN) || contentType.isCompatibleWith(MediaType.TEXT_HTML)
            || contentType.isCompatibleWith(MediaType.TEXT_XML)
            || contentType.isCompatibleWith(MediaType.APPLICATION_XML) || "text".equals(contentType.getType())
            || ("application".equals(contentType.getType()) && contentType.getSubtype().contains("json"));
    }

    /**
     * 截断过长的响应体
     */
    private String truncateBody(String body) {
        if (body == null) {
            return "[null]";
        }
        if (body.length() > MAX_BODY_LENGTH) {
            return body.substring(0, MAX_BODY_LENGTH) + "... [已截断，总长度: " + body.length() + " 字符]";
        }
        return body;
    }
}
