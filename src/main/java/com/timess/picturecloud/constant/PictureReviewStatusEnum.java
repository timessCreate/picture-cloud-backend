package com.timess.picturecloud.constant;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * @author 33363
 * 图片审核状态枚举类
 */

@Getter
public enum PictureReviewStatusEnum {
    REVIEWING("待审核", 0),

    PASS("通过", 1),

    REJECT("拒绝", 2);

    private final String text;
    private final int value;

    PictureReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举对象
     * @param value
     * @return
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value){
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            if(pictureReviewStatusEnum.value == value){
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }

}
