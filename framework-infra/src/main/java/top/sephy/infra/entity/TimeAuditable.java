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
package top.sephy.infra.entity;

import java.time.LocalDateTime;

/**
 * 时间审计接口，表示实体具有创建时间和更新时间
 *
 * @author sephy
 */
public interface TimeAuditable {

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    LocalDateTime getCreatedTime();

    /**
     * 设置创建时间
     *
     * @param createdTime 创建时间
     */
    void setCreatedTime(LocalDateTime createdTime);

    /**
     * 获取更新时间
     *
     * @return 更新时间
     */
    LocalDateTime getUpdatedTime();

    /**
     * 设置更新时间
     *
     * @param updatedTime 更新时间
     */
    void setUpdatedTime(LocalDateTime updatedTime);
}
