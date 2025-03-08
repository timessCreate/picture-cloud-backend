package com.timess.picturecloud.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.timess.picturecloud.config.CosClientConfig;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
import com.timess.picturecloud.manager.CosManager;
import com.timess.picturecloud.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author 33363
 * 图片上传模板
 */
@Service
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param inputSource  输入源对象
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传文件信息响应类VO
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        //1. 校验图片
        validPicture(inputSource);
        //2. 图片上传地址=[]
        String uuid = RandomUtil.randomString(16);
        String originFileName = getOriginalFilename(inputSource);
        //自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFileName));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        //解析结果并返回
        File file = null;
        try {
            //3.创建临时文件，获取文件到服务器
            file = File.createTempFile(uploadPath, null);
            //处理文件来源
            processFile(inputSource, file);
            //4.上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //获取对象存储的返回值
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            return buildResult(imageInfo,uploadPath,originFileName, file);
        } catch (Exception e) {
            log.error("图片存储到cos失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //删除用户上传到内存中的文件
            deleteTempFile(file);
        }
    }

    /**
     * 获取输入源的原始文件名
     * @return
     */
    abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     * @param inputSource
     * @param file
     * @return
     * @throws Exception
     */
    abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 校验输入源(本地文件或url)
     * @param inputSource
     */
    abstract void validPicture(Object inputSource);


    /**
     * 删除临时文件
     * @param file
     */
    private static void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        //删除临时文件
        Boolean delete = file.delete();
        if (!delete) {
            log.error("file delete error, filepath = {}", FileUtil.getAbsolutePath(file));
        }
    }

    /**
     * 构造返回对象
     * @param imageInfo 对象存储返回的图片信息
     * @param uploadPath 上传路径
     * @param originFileName 源文件名字
     * @param file 临时文件
     * @return 上传结果对象
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, String uploadPath, String originFileName, File file){
        //封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originFileName));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(imageInfo.getWidth());
        uploadPictureResult.setPicHeight(imageInfo.getHeight());
        //宽高比计算
        double picScale = NumberUtil.round(imageInfo.getWidth() * 1.0 / imageInfo.getHeight(), 2).doubleValue();
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        return uploadPictureResult;
    }
}
