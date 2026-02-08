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

import com.baomidou.mybatisplus.annotation.TableLogic;

import lombok.Data;

/**
 * 逻辑删除实体抽象类（含完整审计）
 *
 * @deprecated 建议在具体实体类中按需添加逻辑删除字段：
 *             {@code @TableLogic(value = "0", delval = "id") protected Long deleted;}
 */
@Data
@Deprecated
public abstract class AbstractBaseLogicDeleteEntity extends AbstractAuditableEntity {

    @TableLogic(value = "0", delval = "id")
    protected Long deleted;

}
