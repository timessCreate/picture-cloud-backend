package com.timess.picturecloud.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author 33363
 * 批量导入图片请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {
  
    /**  
     * 搜索词
     */  
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    private String namePrefix;

    private static final long serialVersionUID = 1L;  
}
