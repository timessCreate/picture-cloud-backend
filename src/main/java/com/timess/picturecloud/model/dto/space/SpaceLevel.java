package com.timess.picturecloud.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 33363
 * 空间级别信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceLevel {
    /**
     * 值
     */
    private int value;

    /**
     * 介绍
     */
    private String text;

    /**
     * 最大数量
     */
    private long maxCount;

    /**
     * 最大容量
     */
    private long maxSize;
}
