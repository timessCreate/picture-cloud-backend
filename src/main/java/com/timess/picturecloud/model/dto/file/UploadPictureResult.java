package com.timess.picturecloud.model.dto.file;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 上传图片响应的结果
 * @author 33363
 */

@Data
public class UploadPictureResult implements Serializable {
    /**
     * 图片 url
     */
    private String url;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}