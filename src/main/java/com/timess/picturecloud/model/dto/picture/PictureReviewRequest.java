package com.timess.picturecloud.model.dto.picture;

import lombok.Data;

import java.io.Serializable;


/**
 * @author 33363
 * 审核状态请求类
 */
@Data
public class PictureReviewRequest implements Serializable {
  
    /**  
     * 图片id
     */  
    private Long id;  
  
    /**  
     * 状态：0-待审核, 1-通过, 2-拒绝  
     */  
    private Integer reviewStatus;  
  
    /**  
     * 审核信息  
     */  
    private String reviewMessage;  
  
  
    private static final long serialVersionUID = 1L;  
}
