package com.timess.picturecloud.model.dto.spaceuser;

import com.timess.picturecloud.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询请求
 * @author xing10
 */
@Data
public class SpaceUserQueryRequest implements Serializable {


    /**
     * id
     */
    private Long id;
    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}