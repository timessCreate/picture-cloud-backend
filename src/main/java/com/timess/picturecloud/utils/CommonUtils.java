package com.timess.picturecloud.utils;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * @author 33363
 * 工具类
 */
public class CommonUtils {

    /**
     * 密码加密
     * @param userPassword
     * @return
     */
    public static String getEncryptPassword(String userPassword){
        final String SALT = "timess";
        return DigestUtils.md5DigestAsHex(
                (SALT + userPassword).getBytes(StandardCharsets.UTF_8));
    }
}
