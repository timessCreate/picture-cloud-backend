package com.timess.picturecloud.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 33363
 * 用户注册和登录公用接口，注册时，前端需要对输入密码和确认密码进行比较，相同再发送请求
 *
 */
@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = -4839454500897686724L;

    String userAccount;

    String userPassword;

}
