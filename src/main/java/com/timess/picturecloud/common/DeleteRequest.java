package com.timess.picturecloud.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 33363
 * 删除请求类
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 7794472253139139229L;
    /**
     * id
     */
    private Long id;
}
