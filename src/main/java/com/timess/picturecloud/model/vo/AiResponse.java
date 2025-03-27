package com.timess.picturecloud.model.vo;

import lombok.Data;

/**
 * @author xing10
 */
@Data
public class AiResponse {

    /**
     * 生成图表 --> 适合Echarts V5显示的代码
     */
    private String genChart;

    /**
     * ai分析结果
     */
    private String genResult;

    /**
     * 新生成的图表id
     */
    private Long chartId;
}
