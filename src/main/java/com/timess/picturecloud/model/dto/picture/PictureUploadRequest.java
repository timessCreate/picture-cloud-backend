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
    private static final long serialVersionUID = -2553608705307086873L;
}
