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
package top.sephy.infra.webclient;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import reactor.core.publisher.Mono;
import top.sephy.infra.consts.HttpHeaderConstants;
import top.sephy.infra.utils.ThreadContextUtils;

/**
 * 将当前请求上下文中的 traceId 透传到 WebClient 请求头。
 *
 * @author sephy
 */
public class WebClientTraceIdFilter implements ExchangeFilterFunction {

    public static final String TRACE_ID_HEADER = HttpHeaderConstants.HEADER_TRACE_ID;

    public static final String MDC_TRACE_ID_KEY = "tid";

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String existingHeaderTraceId = request.headers().getFirst(TRACE_ID_HEADER);
        if (StringUtils.isNotBlank(existingHeaderTraceId)) {
            return next.exchange(request);
        }

        String traceId = resolveTraceId();
        if (StringUtils.isBlank(traceId)) {
            return next.exchange(request);
        }

        ClientRequest requestWithTraceId =
            ClientRequest.from(request).headers(headers -> headers.set(TRACE_ID_HEADER, traceId)).build();
        return next.exchange(requestWithTraceId);
    }

    private String resolveTraceId() {
        String traceId = ThreadContextUtils.getTraceId();
        if (StringUtils.isBlank(traceId)) {
            traceId = MDC.get(MDC_TRACE_ID_KEY);
        }
        return StringUtils.trimToNull(traceId);
    }
}
