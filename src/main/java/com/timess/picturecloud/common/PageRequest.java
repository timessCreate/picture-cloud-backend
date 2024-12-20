package com.timess.picturecloud.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 33363
 * 分页请求类
 */

@Data
public class PageRequest implements Serializable{

    private static final long serialVersionUID = 1640194160662576868L;
    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序
     */
    private String sortOrder = "descend";
}
