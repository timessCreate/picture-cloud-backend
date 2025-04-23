package com.timess.picturecloud.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 发送验证码接口
 * @author 33363
 */
@Data
public class UserSendRegisterMailRequest implements Serializable {
    /**
     * 用户邮箱
     */
    private String mail;

    private static final long serialVersionUID = 1L;
}
