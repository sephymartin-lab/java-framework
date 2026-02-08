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
 * 可标识接口（逻辑主键），表示实体具有逻辑主键（uid）
 *
 * @param <UID> 逻辑主键类型
 * @author sephy
 */
public interface UidIdentifiable<UID> {

    /**
     * 获取逻辑主键
     *
     * @return 逻辑主键值
     */
    UID getUid();

    /**
     * 设置逻辑主键
     *
     * @param uid 逻辑主键值
     */
    void setUid(UID uid);
}
