package com.timess.picturecloud.exception;

/**
 * @author 33363
 * 异常处理工具类
 */
public class ThrowUtils {
    /**
     * 条件成立则抛出异常
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException){
        if(condition){
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛出异常
     * @param condition
     * @param errorCode
     */
    public static void throwIf(boolean condition,  ErrorCode errorCode){
       throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件成立则抛出异常
     * @param condition 判定条件
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public static void throwIf(boolean condition,  ErrorCode errorCode, String message){
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
