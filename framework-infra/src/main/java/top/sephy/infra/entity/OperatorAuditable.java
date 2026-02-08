/*
 * Copyright 2022-2026 sephy.top
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
 * limitations under the License.
 */
package top.sephy.infra.entity;

/**
 * 操作人审计接口，表示实体具有创建人和更新人
 *
 * @author sephy
 */
public interface OperatorAuditable {

    /**
     * 获取创建人ID
     *
     * @return 创建人ID
     */
    Long getCreatedBy();

    /**
     * 设置创建人ID
     *
     * @param createdBy 创建人ID
     */
    void setCreatedBy(Long createdBy);

    /**
     * 获取更新人ID
     *
     * @return 更新人ID
     */
    Long getUpdatedBy();

    /**
     * 设置更新人ID
     *
     * @param updatedBy 更新人ID
     */
    void setUpdatedBy(Long updatedBy);
}
