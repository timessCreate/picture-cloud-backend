package com.timess.picturecloud.model.enums;

import cn.hutool.core.util.ObjUtil;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
import lombok.Getter;

/**
 * @author 33363
 * 团队空间类型枚举类
 */

@Getter
public enum SpaceTypeEnum {

    /**
     * 私人空间
     */
    PRIVATE("私有空间",0),

    /**
     * 团队空间
     */
    TEAM("团队空间",1);

    private final String text;
    private final int value;


    /**
     *
     * @param text 描述
     * @param value 值
     */
     SpaceTypeEnum(String text, int value){
        this.value = value;
        this.text = text;
    }
    /**
     * 根据 value 获取枚举
     */
    public static SpaceTypeEnum getEnumByValue(Integer value) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(value), ErrorCode.PARAMS_ERROR, "空间级别不存在");
        for (SpaceTypeEnum spaceTypeEnum : SpaceTypeEnum.values()) {
            if (spaceTypeEnum.value == value) {
                return spaceTypeEnum;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不存在");
    }
}

