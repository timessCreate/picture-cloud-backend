package com.timess.picturecloud.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * @author 33363
 * url上传图片
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate{
    @Override
    String getOriginalFilename(Object inputSource) {
        String pictureUrl = (String) inputSource;
        return FileUtil.mainName(pictureUrl);
    }

    @Override
    void processFile(Object inputSource, File file) throws Exception {
        String pictureUrl = (String) inputSource;
        HttpUtil.downloadFile(pictureUrl, file);
    }

    @Override
    void validPicture(Object inputSource) {
        String pictureUrl = (String) inputSource;
        //url地址判空
        ThrowUtils.throwIf(StrUtil.isBlank(pictureUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        //校验url地址是否正确
        try {
            new URL(pictureUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件格式地址不正确");
        }
        //校验协议是否为https 或 http
        if (!(StringUtils.startsWith(pictureUrl, "https://") || StringUtils.startsWith(pictureUrl, "http://"))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持http或https协议");
        }

        //发送HEAD请求以验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, pictureUrl).execute();
            //没有正常返回，无需执行其他判断
            if(response.getStatus() != HttpStatus.HTTP_OK){
                return;
            }
            //验证文件类型
            String contentType = response.header("Content-Length");
            if(StrUtil.isNotBlank(contentType)){
                //允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jpg", "image/webp");
                ThrowUtils.throwIf(ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR,"图片类型错误");
            }
            //检验文件大小
            String contentLengthStr = response.header("Content-Length");
            if(StrUtil.isNotBlank(contentLengthStr)){
                try{
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long FIVE_MB = 1024 * 1024 * 3L;
                    ThrowUtils.throwIf(contentLength > FIVE_MB,
                            ErrorCode.PARAMS_ERROR,
                            "图片大小不能超过3MB");
                }catch (NumberFormatException e){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        }finally {
            if(response != null){
                response.close();
            }
        }
    }
}
