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

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.Data;
import top.sephy.infra.mybatis.audit.annotaton.CreatedTime;
import top.sephy.infra.mybatis.audit.annotaton.ModifiedTime;

/**
 * 基础实体抽象类
 *
 * 包含字段：id（自增主键）、createdTime（创建时间）、updatedTime（更新时间）
 *
 * 适用场景：系统表、配置表、C端数据、无需操作人审计的场景
 *
 * @author sephy
 */
@Data
public abstract class AbstractBaseEntity implements Serializable, Identifiable<Long>, TimeAuditable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    protected Long id;

    /**
     * 创建时间
     */
    @CreatedTime
    protected LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @ModifiedTime
    protected LocalDateTime updatedTime;
}
