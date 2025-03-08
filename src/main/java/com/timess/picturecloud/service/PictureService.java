package com.timess.picturecloud.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timess.picturecloud.model.domain.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.dto.picture.PictureQueryRequest;
import com.timess.picturecloud.model.dto.picture.PictureReviewRequest;
import com.timess.picturecloud.model.dto.picture.PictureUploadRequest;
import com.timess.picturecloud.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author 33363
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2024-12-31 22:26:29
*/
public interface PictureService extends IService<Picture> {

    /**
     * 图片上传
     * @param inputSource
     * @param pictureUploadRequest 上传图片的id
     * @param loginUser 当前登录用户
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 将查询请求转化为QueryWrapper对象
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片封装对象
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 编写图片数据校验方法
     * @param picture
     */
    void validPicture(Picture picture);


    /**
     * Picture review
      * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * Fill in the review parameter information of the picture table
     * @param picture
     * @param logUser
     */
    void fillReviewParams(Picture picture, User logUser);

    /**
     * 清理指定图片记录
     * @param oldPicture
     */
    void deletePictureFile(Picture oldPicture);
}
