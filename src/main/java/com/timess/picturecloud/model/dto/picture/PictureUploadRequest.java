package com.timess.picturecloud.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 33363
 * 上传图片
 */
@Data
public class PictureUploadRequest implements Serializable {
    /**
     * 图片id
     */
    private Long id;

    /**
     * 图片url地址
     */
    private String pictureUrl;
    /**
     * 图片上传指定库id
     */
    private Long spaceId;

    /**
     * 图片名称
     */
    private String picName;

    private static final long serialVersionUID = -2553608705307086873L;
}
