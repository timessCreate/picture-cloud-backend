package com.timess.picturecloud.common;

import com.timess.picturecloud.exception.ErrorCode;

/**
 * @author 33363
 * 响应工具类
 */
public class ResultUtils{
    /**
     * 成功
     * @param data
     * @param <T>
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0, "ok", data);
    }

    /**
     * 失败
     * @param errorCode
     * @return
     */
    public static BaseResponse<?> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     * @param code
     * @param message
     * @return
     */
    public static BaseResponse<?> error(int code, String message){
        return new BaseResponse<>(code, message, "");
    }

    /**
     * 失败
     * @param errorCode
     * @param message
     * @return
     */
    public static BaseResponse<?> error(ErrorCode errorCode, String message){
        return new BaseResponse<>(errorCode.getCode(), message, null);
    }
}
