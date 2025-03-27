package com.timess.picturecloud.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 *
 *  图表上传
 * @author xing10
 */
@Data
public class GenChartByAiRequest implements Serializable {

    /**
     * 图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}