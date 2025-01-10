package com.timess.picturecloud.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.timess.picturecloud.annotation.AuthCheck;
import com.timess.picturecloud.common.BaseResponse;
import com.timess.picturecloud.common.ResultUtils;
import com.timess.picturecloud.constant.UserConstant;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @author 33363
 * 文件上传
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;
    @AuthCheck(anyRole = {UserConstant.SUPER_ADMIN_ROLE, UserConstant.ADMIN_ROLE})
    @PostMapping("/test/upload")
    public BaseResponse<String> testUpdateFile(@RequestPart("file")MultipartFile multipartFile){
        //定义文件目录
        String fileName = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s",fileName);
        //将multipartFile转换成file
        File file = null;
        try {
            file = File.createTempFile(filePath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filePath, file);
            //返回图片访问地址
            return ResultUtils.success(filePath);

        } catch (IOException e) {
            log.error("file upload error, filePath = {}" + filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }finally {
            if(file != null){
                //删除临时文件
                boolean delete = file.delete();
                if(!delete){
                   log.error("file delete error, file path = {}" + filePath);
                }
            }
        }
    }

    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPER_ADMIN_ROLE})
    @GetMapping("/test/download")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream objectContent = null;
        try {
            //获取文件对象
            COSObject object = cosManager.getObject(filepath);
            //获取实际内容-->得到一个输入流
            objectContent = object.getObjectContent();
            byte[] byteArray = IOUtils.toByteArray(objectContent);
            //流式传输给前端，响应结果设置
            // 设置响应头
            //application/octet-stream 是一种通用的二进制流MIME类型，标识服务器返回的是一个二进制文件或位置类型的文件
            //提示浏览器不对该文件进行展示，而是下载到本地
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(byteArray);
            // 刷新缓冲区
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file upload error, filePath = {}" + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        }finally {
            //关闭流
            if(objectContent != null){
                objectContent.close();
            }
        }

    }
}


