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

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.sephy.infra.consts.GlobalSystemConstants;
import top.sephy.infra.exception.ServiceException;

/**
 * @author sephy
 * @date 2020-06-30 21:32
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = ServiceException.class)
    @ResponseBody
    public CommonResult<Object> serviceExceptionHandler(HttpServletRequest req, HttpServletResponse resp,
        ServiceException e) {
        log.warn("业务异常", e);
        req.setAttribute(GlobalSystemConstants.ATTR_EXCEPTION, e);
        return CommonResult.error("BIZ_ERROR", e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public CommonResult<Object> exceptionHandler(HttpServletRequest req, HttpServletResponse resp, Exception e) {
        log.error("系统异常", e);
        req.setAttribute(GlobalSystemConstants.ATTR_EXCEPTION, e);
        // 500 用于 prometheus 监控
        resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return CommonResult.error("SYS_ERROR", e.getMessage());
    }
}
