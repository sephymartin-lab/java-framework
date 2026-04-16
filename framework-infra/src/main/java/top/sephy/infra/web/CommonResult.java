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
package top.sephy.infra.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CommonResult<T> {

    private static CommonResult<Void> SUCCESS = success(null);

    public static final int CODE_SUCCESS = 0;

    public static final int CODE_ERROR = 1;

    private int code;

    private String error;

    private String message;

    private long timestamp;

    private T data;

    private CommonResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static CommonResult<Void> success() {
        return SUCCESS;
    }

    public static <E> CommonResult<E> success(E data) {
        return new CommonResult<>(CODE_SUCCESS, null, data);
    }

    public static <E> CommonResult<E> error(String error, String message) {
        CommonResult<E> result = new CommonResult<>(CODE_ERROR, message, null);
        result.setError(error);
        return result;

    }
}
