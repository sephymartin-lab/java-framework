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
package top.sephy.infra.paging;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class PagingResult<T> implements Iterable<T> {

    // 页码
    private int pageNum;

    // 分页大小
    private int pageSize;

    // 总页数
    private int totalPages;

    // 总记录数
    private long total;

    // 分页内容
    private List<T> list;

    @JsonCreator
    public PagingResult(@JsonProperty("list") List<T> list, @JsonProperty("pageNum") int pageNum,
        @JsonProperty("pageSize") int pageSize, @JsonProperty("total") long total) {
        Assert.isTrue(pageNum > 0, "pageNum must be positive.");
        Assert.isTrue(pageSize > 0, "pageSize must be positive.");
        Assert.isTrue(total >= 0, "totalElements must net be negative.");
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.totalPages = (int)(total / pageSize + (total % pageSize == 0 ? 0 : 1));
        this.list = list == null ? Collections.EMPTY_LIST : list;
    }

    /**
     * 分页内容记录数
     * 
     * @return
     */
    public int getNumberOfElements() {
        return list.size();
    }

    /**
     * 是否有上一页
     * 
     * @return
     */
    public boolean hasPrevious() {
        return pageNum > 1;
    }

    /**
     * 是否有下一页
     * 
     * @return
     */
    public boolean hasNext() {
        return pageNum < totalPages;
    }

    /**
     * 是否是第一页
     * 
     * @return
     */
    public boolean isFirst() {
        return !hasPrevious();
    }

    /**
     * 是否是最后一页
     * 
     * @return
     */
    public boolean isLast() {
        return !hasNext();
    }

    /**
     * 是否有分页内容
     * 
     * @return
     */
    public boolean hasContent() {
        return !list.isEmpty();
    }

    @Override
    public @NonNull Iterator<T> iterator() {
        return list.iterator();
    }

    /**
     * 从 MyBatis-Plus IPage 对象转换
     *
     * @param page MyBatis-Plus 分页结果
     * @param <T>  实体类型
     * @return 分页结果
     */
    public static <T> PagingResult<T> from(IPage<T> page) {
        return new PagingResult<>(page.getRecords(), (int)page.getCurrent(), (int)page.getSize(), page.getTotal());
    }

    /**
     * 从 MyBatis-Plus IPage 对象转换，支持类型转换
     *
     * @param page      MyBatis-Plus 分页结果
     * @param converter 类型转换函数 (DO -> VO)
     * @param <S>       源类型
     * @param <T>       目标类型
     * @return 分页结果
     */
    public static <S, T> PagingResult<T> from(IPage<S> page, Function<S, T> converter) {
        List<T> list = page.getRecords().stream()
            .map(converter)
            .collect(Collectors.toList());
        return new PagingResult<>(list, (int)page.getCurrent(), (int)page.getSize(), page.getTotal());
    }

    /**
     * 从 MyBatis-Plus IPage 对象转换，使用已转换的列表
     *
     * @param page          MyBatis-Plus 分页结果（用于获取分页信息）
     * @param convertedList 已转换的列表
     * @param <S>           源类型
     * @param <T>           目标类型
     * @return 分页结果
     */
    public static <S, T> PagingResult<T> from(IPage<S> page, List<T> convertedList) {
        return new PagingResult<>(convertedList, (int)page.getCurrent(), (int)page.getSize(), page.getTotal());
    }

    /**
     * 创建空的分页结果
     *
     * @param pageNum  页码
     * @param pageSize 分页大小
     * @param <T>      实体类型
     * @return 空的分页结果
     */
    public static <T> PagingResult<T> empty(int pageNum, int pageSize) {
        return new PagingResult<>(Collections.emptyList(), pageNum, pageSize, 0);
    }

    /**
     * 创建空的分页结果
     *
     * @param query 分页查询对象
     * @param <T>   实体类型
     * @return 空的分页结果
     */
    public static <T> PagingResult<T> empty(PagingQuery query) {
        return new PagingResult<>(Collections.emptyList(), query.getPageNum(), query.getPageSize(), 0);
    }
}
