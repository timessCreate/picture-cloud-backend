package com.timess.picturecloud.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 33363
 */
@Data
public class SpaceEditRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    private static final long serialVersionUID = 1L;
}

