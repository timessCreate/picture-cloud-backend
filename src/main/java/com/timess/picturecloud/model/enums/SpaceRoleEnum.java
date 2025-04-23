package com.timess.picturecloud.model.enums;

import cn.hutool.core.util.ObjUtil;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
import lombok.Getter;

/**
 * @author 33363
 * 团队空间角色枚举类
 */

@Getter
public enum SpaceRoleEnum {

    VIEWER("浏览者", "viewer"),
    EDITOR("编辑者", "editor"),
    ADMIN("管理员", "admin");

    private final String text;
    private final String value;


    /**
     *
     * @param text 描述
     * @param value 值
     */
     SpaceRoleEnum(String text, String value){
        this.value = value;
        this.text = text;
    }
    /**
     * 根据 value 获取枚举
     */
    public static SpaceRoleEnum getEnumByValue(String value) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(value), ErrorCode.PARAMS_ERROR, "空间级别不存在");
        for (SpaceRoleEnum spaceTypeEnum : SpaceRoleEnum.values()) {
            if (spaceTypeEnum.value.equals(value)) {
                return spaceTypeEnum;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
    }
}

