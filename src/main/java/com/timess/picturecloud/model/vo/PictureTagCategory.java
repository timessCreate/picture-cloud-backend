package com.timess.picturecloud.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author 33363
 * 图片标签分类
 */

@Data
public class PictureTagCategory implements Serializable {

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 分类列表
     */
    private List<String> categoryList;

    private static final long serialVersionUID = -2924080948004101321L;
}
