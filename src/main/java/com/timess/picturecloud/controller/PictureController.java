package com.timess.picturecloud.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.timess.picturecloud.annotation.AuthCheck;
import com.timess.picturecloud.common.BaseResponse;
import com.timess.picturecloud.common.DeleteRequest;
import com.timess.picturecloud.common.ResultUtils;
import com.timess.picturecloud.constant.PictureReviewStatusEnum;
import com.timess.picturecloud.constant.UserConstant;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
import com.timess.picturecloud.model.domain.Picture;
import com.timess.picturecloud.model.domain.Space;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.dto.picture.*;
import com.timess.picturecloud.model.vo.PictureTagCategory;
import com.timess.picturecloud.model.vo.PictureVO;
import com.timess.picturecloud.service.PictureService;
import com.timess.picturecloud.service.SpaceService;
import com.timess.picturecloud.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 33363
 * 图片上传
 */
@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 本地缓存
     */
    Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000L)
            //缓存5分钟后过期
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /**
     * upload picture
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(
            @ModelAttribute PictureUploadRequest pictureUploadRequest,
            @RequestParam("file")MultipartFile multipartFile,
            HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/uploadByUrl")
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request){
        String pictureUrl = pictureUploadRequest.getPictureUrl();
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(pictureUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(loginUser, deleteRequest.getId());
        return ResultUtils.success(true);
    }
    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(anyRole = {UserConstant.SUPER_ADMIN_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //supplement additional review parameters
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(anyRole = {UserConstant.SUPER_ADMIN_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        if(spaceId != null){
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(loginUser, picture);
        }
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(anyRole = {UserConstant.SUPER_ADMIN_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        //set the review status of pictureQueryRequest class to pass
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Long spaceId = pictureQueryRequest.getSpaceId();
        if(spaceId == null){
            //公开图库
            //普通用户只能看到审核通过后的数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        }else {
            //私有空间
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if(!loginUser.getId().equals(space.getUserId())){
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR,"无权限查询该私有空间");
            }
        }
        //查询本地缓存，缓存中没有，再查询分布式缓存
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        //构建缓存key
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes(StandardCharsets.UTF_8));
        String redisKey = String.format("yitu:listPictureVoByPage:%s", hashKey);
        String localCacheKey = String.format("listPictureVoByPage:%s", hashKey);
        String cacheValue = LOCAL_CACHE.getIfPresent(localCacheKey);
//        //如果命中本地缓存
//        if(cacheValue != null){
//            log.info("命中本地缓存：" + cacheValue);
//            Page<PictureVO> localCachePage = JSONUtil.toBean(cacheValue, Page.class);
//            return ResultUtils.success(localCachePage);
//        }
        //查询缓存，缓存中没有，再查询数据库
        //获取String类型操作对象
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
//        String redisValue = valueOps.get(redisKey);
//        if(redisValue != null){
//            //命中缓存,则直接返回结果,并更新本地缓存
//            log.info("命中分布式缓存：" + redisValue);
//            LOCAL_CACHE.put(localCacheKey, redisValue);
//            Page<PictureVO> cachePage = JSONUtil.toBean(redisValue, Page.class);
//            return ResultUtils.success(cachePage);
//        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        //添加到本地缓存中
        LOCAL_CACHE.put(localCacheKey, JSONUtil.toJsonStr(pictureVOPage));
        //添加到缓存中, 同时为了避免缓存雪崩，为每一键值设置为5-10分钟内随机过期时间
        int expireTime = 300 + RandomUtil.randomInt(300);
        valueOps.set(redisKey, JSONUtil.toJsonStr(pictureVOPage), expireTime, TimeUnit.SECONDS);
        //返回封装类
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 分页获取图片列表（封装类） + redis缓存 + caffeine本地缓存
     */
    @PostMapping("/list/page/vo/cache")
    @Deprecated
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        //set the review status of pictureQueryRequest class to pass
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //查询本地缓存，缓存中没有，再查询分布式缓存
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        //构建缓存key
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes(StandardCharsets.UTF_8));
        String redisKey = String.format("yitu:listPictureVoByPage:%s", hashKey);
        String localCacheKey = String.format("listPictureVoByPage:%s", hashKey);
        String cacheValue = LOCAL_CACHE.getIfPresent(localCacheKey);
        //如果命中本地缓存
        if(cacheValue != null){
            log.info("命中本地缓存：" + cacheValue);
            Page<PictureVO> localCachePage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(localCachePage);
        }
        //查询缓存，缓存中没有，再查询数据库
        //获取String类型操作对象
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        String redisValue = valueOps.get(redisKey);
        if(redisValue != null){
            //命中缓存,则直接返回结果,并更新本地缓存
            log.info("命中分布式缓存：" + redisValue);
            LOCAL_CACHE.put(localCacheKey, redisValue);
            Page<PictureVO> cachePage = JSONUtil.toBean(redisValue, Page.class);
            return ResultUtils.success(cachePage);
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        //添加到本地缓存中
        LOCAL_CACHE.put(localCacheKey, JSONUtil.toJsonStr(pictureVOPage));
        //添加到缓存中, 同时为了避免缓存雪崩，为每一键值设置为5-10分钟内随机过期时间
        int expireTime = 300 + RandomUtil.randomInt(300);
        valueOps.set(redisKey, JSONUtil.toJsonStr(pictureVOPage), expireTime, TimeUnit.SECONDS);
        //返回封装类
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        pictureService.validPicture(picture);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        pictureService.checkPictureAuth(loginUser, oldPicture);
        //update review parameters
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意","人像");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * picture review interface
     * @param pictureReviewRequest review request class
     * @param request
     * @return true
     */
    @PostMapping("/review")
    @AuthCheck(anyRole = {UserConstant.SUPER_ADMIN_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request){
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser =  userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }
}



