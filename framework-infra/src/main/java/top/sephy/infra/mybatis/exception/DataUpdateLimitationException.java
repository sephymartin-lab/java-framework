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
package top.sephy.infra.mybatis.exception;

import top.sephy.infra.exception.SystemException;

public class DataUpdateLimitationException extends SystemException {

    public DataUpdateLimitationException(String message) {
        super(message);
    }

    public static DataUpdateLimitationException DEFAULT =
        new DataUpdateLimitationException("本次操作 因超过系统安全阈值 被拦截，如需继续，请联系管理员!");
}
