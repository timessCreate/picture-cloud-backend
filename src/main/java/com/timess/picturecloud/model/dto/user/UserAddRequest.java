package com.timess.picturecloud.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增用户接口
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户邮箱
     */
    private String mail;

    /**
     * 用户密码
     */
    private String userPassword;
    /**
     * 验证码
     */
    private String verifyCode;
    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
