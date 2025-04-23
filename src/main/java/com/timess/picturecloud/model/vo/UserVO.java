package com.timess.picturecloud.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 获取用户信息
 * @author 33363
 */
@Data
public class UserVO implements Serializable {

    /**
     * id
     */
    private Long id;
    
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户邮箱
     */
    private String mail;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 用户状态
     */
    private int status;
    private static final long serialVersionUID = 1L;
}
