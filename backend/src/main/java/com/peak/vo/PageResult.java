package com.peak.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 页码分页包装（pageNum/pageSize 模式）
 *
 * <pre>
 * { "list": [...], "total": 128, "pages": 7, "pageNum": 1, "pageSize": 20 }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    /** 当前页数据列表 */
    private List<T> list;
    /** 总记录数 */
    private long total;
    /** 总页数 */
    private long pages;
    /** 当前页码（从 1 开始） */
    private int pageNum;
    /** 每页条数 */
    private int pageSize;
}
