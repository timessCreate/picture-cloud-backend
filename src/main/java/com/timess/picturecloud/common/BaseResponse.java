package com.timess.picturecloud.common;

import com.timess.picturecloud.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * @author 33363
 * 全局响应类
 */
@Data
public class BaseResponse<T> implements Serializable {

    private static final long serialVersionUID = 6716332470891757076L;
    /**
     * 自定义响应码
     */
    private int code;

    /**
     * 信息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    public BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public BaseResponse(int code,T data) {
       this(code, "",data);
    }

    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(), errorCode.getMessage(), null);
    }

}
