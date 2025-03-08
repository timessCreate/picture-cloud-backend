package com.timess.picturecloud.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.timess.picturecloud.config.CosClientConfig;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
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
 * 文件上传下载操作类
 */

@Service
@Slf4j
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传文件信息响应类VO
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        //校验图片
        validPicture(multipartFile);
        //图片上传地址=[]
        String uuid = RandomUtil.randomString(16);
        String originFileName = multipartFile.getOriginalFilename();
        //自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFileName));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        //解析结果并返回
        File file = null;
        try {
            //上传文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //获取上传的图片信息对象

            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            //封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originFileName));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(imageInfo.getWidth());
            uploadPictureResult.setPicHeight(imageInfo.getHeight());
            //宽高比计算
            double picScale = NumberUtil.round(imageInfo.getWidth() * 1.0 / imageInfo.getHeight(), 2).doubleValue();
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片存储到cos失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //删除用户上传到内存中的文件
            deleteTempFile(file);
        }
    }

    /**
     * 检验文件是否有效
     *
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        //1. 检验文件大小
        long fileSize = multipartFile.getSize();
        //限制文件大小
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > ONE_M * 3, ErrorCode.PARAMS_ERROR, "文件不能大于3MB");
        //2. 校验文件后缀 --> 根据文件的原始名称
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀列表(或者集合)
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(suffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    /**
     * 删除临时文件
     *
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
     * 图片URL地址导入
     *
     * @param pictureUrl       图片url
     * @param uploadPathPrefix 上传路径前缀
     * @return 图片信息
     */
    public UploadPictureResult uploadPictureByUrl(String pictureUrl, String uploadPathPrefix) {
        //URL验证
        validPictureUrl(pictureUrl);
        //图片上传地址=[]
        String uuid = RandomUtil.randomString(16);
        String originFileName = FileUtil.mainName(pictureUrl);
        //自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFileName));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        //解析结果并返回
        File file = null;
        try {
            //上传文件
            file = File.createTempFile(uploadPath, null);
            HttpUtil.downloadFile(pictureUrl, file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //获取上传的图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originFileName));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(imageInfo.getWidth());
            uploadPictureResult.setPicHeight(imageInfo.getHeight());
            //宽高比计算
            double picScale = NumberUtil.round(imageInfo.getWidth() * 1.0 / imageInfo.getHeight(), 2).doubleValue();
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片存储到cos失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //删除用户上传到内存中的文件
            deleteTempFile(file);
        }
    }

    /**
     * 验证URL是否可用
     * @param pictureUrl
     */
    public void validPictureUrl(String pictureUrl) {
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
                    final long FIVE_MB = 1024 * 1024 * 5L;
                    ThrowUtils.throwIf(contentLength > FIVE_MB,
                            ErrorCode.PARAMS_ERROR,
                            "图片大小不能超过5MB");
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
