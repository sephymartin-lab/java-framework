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
package top.sephy.infra.mybatis.plus;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;

import io.micrometer.common.util.StringUtils;
import top.sephy.infra.exception.SystemException;

/**
 * @author sephy
 * @date 2020-03-07 23:33
 */
public interface CustomBaseMapper<T> extends BaseMapper<T> {

    default T loadById(Serializable id, String errorMsg) {
        T obj = this.selectById(id);
        if (obj != null) {
            return obj;
        }
        throw new SystemException(StringUtils.isBlank(errorMsg) ? "数据不存在" : errorMsg);
    }

    /**
     * 全字段插入
     * 
     * @param entity
     * @return
     */
    int insertAllColumns(T entity);

    /**
     * 多个插入, insert values 模式, 忽略自增主键
     * 
     * @param entityList
     * @return
     */
    int insertBatchSomeColumn(Collection<T> entityList);

    /**
     * 多个插入, insert values 模式, 会插入所有字段(包含主键字段)
     *
     * @param entityList
     * @return
     */
    @Deprecated
    default int insertAllColumnList(@Param("entityList") Collection<T> entityList) {
        return this.batchInsertAllColumn(entityList);
    }

    int batchInsertAllColumn(@Param("entityList") Collection<T> entityList);

    int alwaysUpdateSomeColumnById(@Param(Constants.ENTITY) T entity);

    int batchUpdateById(@Param("entityList") Collection<T> entityList);

    /**
     * 查询全表
     * 
     * @return
     */
    default List<T> selectAll() {
        return PageHelper.offsetPage(0, 10000).doSelectPage(() -> this.selectList(Wrappers.emptyWrapper()));
    }
}
