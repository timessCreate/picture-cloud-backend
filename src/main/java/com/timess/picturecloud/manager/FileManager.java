package com.timess.picturecloud.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.timess.picturecloud.config.CosClientConfig;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
import com.timess.picturecloud.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author 33363
 * 文件上传下载操作类
 */

@Service
@Slf4j
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     * @param multipartFile 文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传文件信息响应类VO
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix){
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
        }finally{
            //删除用户上传到内存中的文件
            deleteTempFile(file);
        }
    }

    /**
     * 检验文件是否有效
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile){
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
     * @param file
     */
    private static void deleteTempFile(File file){
        if(file == null){
           return;
        }
        //删除临时文件
        Boolean delete = file.delete();
        if(!delete){
            log.error("file delete error, filepath = {}", FileUtil.getAbsolutePath(file));
        }
    }
}
