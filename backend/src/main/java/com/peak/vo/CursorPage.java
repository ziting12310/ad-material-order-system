package com.peak.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Cursor 游标分页包装 — API.md §1.2
 *
 * <p>统一列表接口 data 结构，禁止 offset/page。</p>
 * <pre>
 * { "list": [...], "nextCursor": "eyJpZCI6MTAwfQ==", "hasMore": true, "total": 128 }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CursorPage<T> {
    /** 当前页数据列表 */
    private List<T> list;
    /** 下一页游标（无更多数据时为 null） */
    private String nextCursor;
    /** 是否还有下一页 */
    private boolean hasMore;
    /** 总记录数（仅首页返回准确值） */
    private Long total;

    public CursorPage(List<T> list, String nextCursor, boolean hasMore) {
        this(list, nextCursor, hasMore, null);
    }
}
