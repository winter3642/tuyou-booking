package com.tuyou.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 通用分页出参
 */
@Data
@AllArgsConstructor
public class PageVO<T> {
    private long total;
    private List<T> records;

    public static <T> PageVO<T> of(long total, List<T> records) {
        return new PageVO<>(total, records);
    }
}
