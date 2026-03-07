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

import lombok.Data;

/**
 * 多租户完整审计实体抽象类
 *
 * 包含字段：id、createdTime、updatedTime、createdBy、updatedBy、tenantId（租户ID）
 *
 * 适用场景：多租户系统中，需要完整审计的业务数据
 *
 * @author sephy
 */
@Data
public abstract class AbstractTenantAuditableEntity extends AbstractAuditableEntity implements TenantAware {

    /**
     * 租户ID
     */
    protected Long tenantId;
}
