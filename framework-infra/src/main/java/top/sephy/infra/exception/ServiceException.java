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
package top.sephy.infra.exception;

import java.io.Serial;

import lombok.Getter;
import lombok.NonNull;

/**
 * @author sephy
 * @date 2020-06-14 00:52
 */
@Getter
public class ServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 3007273321183731025L;

    /** 未显式指定错误码的新异常使用通用业务错误码。 */
    public static final String DEFAULT_ERROR_CODE = "BIZ_ERROR";

    private final String errorCode;

    private ServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public static ServiceException newInstance(@NonNull String message) {
        return new ServiceException(DEFAULT_ERROR_CODE, message, null);
    }

    public static ServiceException newInstance(@NonNull String message, @NonNull Throwable cause) {
        return new ServiceException(DEFAULT_ERROR_CODE, message, cause);
    }

    public static ServiceException newInstance(@NonNull String errorCode, @NonNull String message) {
        return new ServiceException(errorCode, message, null);
    }

    public static ServiceException newInstance(@NonNull String errorCode, @NonNull String message,
        @NonNull Throwable cause) {
        return new ServiceException(errorCode, message, cause);
    }
}
