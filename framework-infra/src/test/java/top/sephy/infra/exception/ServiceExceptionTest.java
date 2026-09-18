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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ServiceExceptionTest {

    @Test
    void messageOverloadsUseDefaultErrorCode() {
        Throwable cause = new IllegalStateException("cause");

        ServiceException messageOnly = ServiceException.newInstance("message");
        ServiceException messageAndCause = ServiceException.newInstance("message", cause);

        assertEquals("message", messageOnly.getMessage());
        assertNull(messageOnly.getCause());
        assertEquals(ServiceException.DEFAULT_ERROR_CODE, messageOnly.getErrorCode());
        assertEquals("message", messageAndCause.getMessage());
        assertEquals(cause, messageAndCause.getCause());
        assertEquals(ServiceException.DEFAULT_ERROR_CODE, messageAndCause.getErrorCode());
    }

    @Test
    void explicitErrorCodeFactoryKeepsErrorCodeMessageAndCause() {
        Throwable cause = new IllegalStateException("cause");

        ServiceException exception = ServiceException.newInstance("USER_EXISTS", "用户已存在", cause);

        assertEquals("USER_EXISTS", exception.getErrorCode());
        assertEquals("用户已存在", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void explicitErrorCodeWithoutCauseIsPreserved() {
        ServiceException exception = ServiceException.newInstance("USER_EXISTS", "用户已存在");

        assertEquals("USER_EXISTS", exception.getErrorCode());
        assertEquals("用户已存在", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> ServiceException.newInstance((String)null));
        assertThrows(NullPointerException.class, () -> ServiceException.newInstance("CODE", (String)null));
        assertThrows(NullPointerException.class,
            () -> ServiceException.newInstance("CODE", "message", null));
    }
}
