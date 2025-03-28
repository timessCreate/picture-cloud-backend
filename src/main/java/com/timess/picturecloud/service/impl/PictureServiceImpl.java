package com.timess.picturecloud.service.impl;
import java.io.IOException;
import java.util.*;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.timess.picturecloud.constant.PictureReviewStatusEnum;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
import com.timess.picturecloud.manager.CosManager;
import com.timess.picturecloud.manager.upload.FilePictureUpload;
import com.timess.picturecloud.manager.upload.PictureUploadTemplate;
import com.timess.picturecloud.manager.upload.UrlPictureUpload;
import com.timess.picturecloud.model.domain.Picture;
import com.timess.picturecloud.model.domain.Space;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.dto.file.UploadPictureResult;
import com.timess.picturecloud.model.dto.picture.PictureQueryRequest;
import com.timess.picturecloud.model.dto.picture.PictureReviewRequest;
import com.timess.picturecloud.model.dto.picture.PictureUploadByBatchRequest;
import com.timess.picturecloud.model.dto.picture.PictureUploadRequest;
import com.timess.picturecloud.model.vo.PictureVO;
import com.timess.picturecloud.model.vo.UserVO;
import com.timess.picturecloud.service.PictureService;
import com.timess.picturecloud.mapper.PictureMapper;
import com.timess.picturecloud.service.SpaceService;
import com.timess.picturecloud.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
* @author 33363
* @description 针对表【picture(图片)】的数据库操作Service实现
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private CosManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 增加或删除图片上传记录到数据库
     * @param inputSource
     * @param pictureUploadRequest 上传图片的id
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);
        //校验空间是否存在， 如果spaceId为null，则默认是公共空间
        Long spaceId = pictureUploadRequest.getSpaceId();
        if(spaceId != null){
            Space space = spaceService.getById(pictureUploadRequest.getSpaceId());
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            //判断是否有空间的权限， 仅空间管理员可以上传
            if(!loginUser.getId().equals(space.getUserId())){
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR,"没有空间权限");
            }
            //检验空间额度
            if(space.getTotalCount() >= space.getMaxCount()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if(space.getTotalSize() >= space.getMaxSize()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        //判断是新增还是删除
        Long pictureId = null;
        if(pictureUploadRequest != null){
            pictureId = pictureUploadRequest.getId();
        }
        //如果id为不为空，查询库中是否包含该条数据
        if(pictureId != null){
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            //Add verification conditions, editable by the owner and administrators only.
            if(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
            //校验空间是否一致
            //没有传spaceId,则复用原有的spaceId(这样也对公共图库进行兼容)
            if(spaceId == null){
                if(oldPicture.getSpaceId() != null){
                    spaceId = oldPicture.getSpaceId();
                }
            }else {
                //用户上传了spaceId，则必须校验请求spaceId和数据库对象的id是否相同
                if(spaceId.equals(oldPicture.getSpaceId())){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求图库id与图片实际所在库id不符");
                }
            }
        }
        //上传图片, 得到图片信息
        //按照用户id划分目录-->按照空间划分目录
        String uploadPathPrefix;
        if(spaceId == null){
            //默认上传到公共空间
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        }else {
            //上传到私有空间
            uploadPathPrefix = String.format("space/%s",spaceId);
        }
        //根据inputSource的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if(inputSource instanceof String){
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        //构造入库对象
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        //如果图片id已经存在，则为更新操作
        if(pictureId != null){
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        picture.setUrl(uploadPictureResult.getUrl());
        String picName = uploadPictureResult.getPicName();
        if(StringUtils.isNotEmpty(pictureUploadRequest.getPicName())){
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //supplement additional review parameters
        fillReviewParams(picture, loginUser);
        //此处涉及多张表的更新，使用编程式事务
        //开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(
                status -> {
                    //更新图片表
                    boolean b = this.saveOrUpdate(picture);
                    ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR, "图片信息上传至数据库失败");
                    //更新空间使用额度
                    boolean update = spaceService.lambdaUpdate()
                            .eq(Space::getId, finalSpaceId)
                            .setSql("totalSize = totalSize + " + picture.getPicSize())
                            .setSql("totalCount = totalCount + 1")
                            .update();
                    
                    //ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "更新空间余额失败");
                    return picture;
                }
        );
        return PictureVO.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        // spaceId为null时查询 IS NULL
        queryWrapper.and(wrapper -> {
            if (spaceId == null){
                wrapper.isNull("spaceId");
            }else{
                wrapper.eq("spaceId", spaceId);
            }
        });
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewMessage), "reviewMessage", reviewMessage);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUserVO(userVO);
        }
        return pictureVO;
    }


    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUserVO(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }


    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * picture review function
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        //If the review status of the request class is not reviewing, an error is reported
        if(id == null || reviewStatus == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //Parameter Check
        //1.Check whether the image exists
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //2. current status is target status
        if(oldPicture.getReviewStatus().equals(reviewStatus)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        //3. Update the user review status
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }
    /**
     * Fill in the review parameter information of the picture table
     * @param picture
     * @param logUser
     */
    @Override
    public void fillReviewParams(Picture picture, User logUser) {
        //If the logged-in user's role is an administrator,
        // then the picture he posts are automatically approved.
        if(userService.isAdmin(logUser)){
            picture.setReviewerId(logUser.getId());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(logUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        }else{
            //Non-administrators, any creation or editing should be changed to "reviewing(0)"
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Async    //异步操作
    @Override
    public void deletePictureFile(Picture oldPicture) {
        //判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        //有不止一条记录使用了该图片
        if(count > 1){
            log.error("有不止一条记录使用了该图片，删除图片文件失败");
            return;
        }
        //删除图片
        cosManager.deleteObject(pictureUrl);
    }

    @Override
    public void deletePicture(User loginUser, long pictureId) {
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        this.checkPictureAuth(loginUser, oldPicture);
        oldPicture.setIsDelete(1);
        // 操作数据库
        Long finalSpaceId = oldPicture.getSpaceId();
        transactionTemplate.execute(
                status -> {
                    //删除mysql中的图片记录
                    boolean b = this.removeById(oldPicture);
                    ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR, "删除数据库中的图片信息失败");
                    //更新空间使用额度
                    if(ObjUtil.isNotEmpty(finalSpaceId)){
                        boolean update = spaceService.lambdaUpdate()
                                .eq(Space::getId, finalSpaceId)
                                .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                                .setSql("totalCount = totalCount - 1")
                                .update();
                        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "更新空间余额失败");
                    }
                    return true;
                }
        );
        this.deletePictureFile(oldPicture);
    }
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if(spaceId == null){
            //公共图片，仅本人或管理员可操作
            if(!picture.getUserId().equals(loginUserId) && !userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "仅本人和管理员可操作");
            }else {
                //私有空间，仅空间管理员可以操纵
                if(!picture.getUserId().equals(loginUserId)){
                    throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "私有空间，仅本人可操作");
                }
            }
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest uploadByBatchRequest, User loginUser) {
        //1. 检验参数
        String searchText = uploadByBatchRequest.getSearchText();
        Integer count = uploadByBatchRequest.getCount();
        int uploadCount = 0;
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "每次最多抓取30张图片");
        //2. 抓取内容地址构建
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);

        Document document;
        try {
             document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "传入的参数异常,");
        }
        //3. 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if(ObjUtil.isEmpty(div)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgList = div.select("img.mimg");
        //遍历元素，依次处理上传图片
        for (Element element : imgList) {
            String fileUrl = element.attr("src");
            if(StringUtils.isBlank(fileUrl)){
                log.info("当前连接为空，已跳过 {}", fileUrl);
                continue;
            }
            //处理图片地址，仅截取"?"前的数据
            int markIndex = fileUrl.indexOf("?");
            if(markIndex > -1){
                fileUrl = fileUrl.substring(0, markIndex);
            }
            //上传图片
            PictureUploadRequest uploadRequest = new PictureUploadRequest();
            uploadRequest.setPictureUrl(fileUrl);
            if(StringUtils.isEmpty(uploadByBatchRequest.getNamePrefix())){
                uploadByBatchRequest.setNamePrefix(uploadByBatchRequest.getSearchText() + RandomUtil.randomNumbers(3));
            }
            uploadRequest.setPicName(uploadByBatchRequest.getNamePrefix());
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, uploadRequest, loginUser);
                log.debug(String.format("图片上传成功, 图片信息为：%s", pictureVO.toString()));
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if(uploadCount >= count){
                break;
            }
        }
        return uploadCount;
    }
}




