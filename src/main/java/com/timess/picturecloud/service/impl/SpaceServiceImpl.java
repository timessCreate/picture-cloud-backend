package com.timess.picturecloud.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
import com.timess.picturecloud.model.domain.Picture;
import com.timess.picturecloud.model.domain.Space;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.dto.space.SpaceAddRequest;
import com.timess.picturecloud.model.dto.space.SpaceQueryRequest;
import com.timess.picturecloud.model.enums.SpaceLevelEnum;
import com.timess.picturecloud.model.vo.PictureVO;
import com.timess.picturecloud.model.vo.SpaceVO;
import com.timess.picturecloud.model.vo.SpaceVO;
import com.timess.picturecloud.model.vo.UserVO;
import com.timess.picturecloud.service.SpaceService;
import com.timess.picturecloud.mapper.SpaceMapper;
import com.timess.picturecloud.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
* @author 33363
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-03-21 15:20:13
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private UserService userService;

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 要创建
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
        }
        // 修改数据时，空间名字长度限制
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
    }
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        // 默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 填充数据
        this.fillSpaceBySpaceLevel(space);
        // 数据校验
        this.validSpace(space, true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 权限校验
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        // 针对用户进行加锁
        //TODO: 可以选择换成分布式锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户仅能有一个私有空间");
                // 写入数据库
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                // 返回新写入的数据 id
                return space.getId();
            });
            // 返回结果是包装类，可以做一些处理
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        queryWrapper.eq(ObjUtil.isNotNull(userId),"userId", userId);
        queryWrapper.like(ObjUtil.isNotNull(spaceName),"spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotNull(spaceLevel),"spaceLevel", spaceLevel);
        return queryWrapper;
    }



    /**
     * 将实体类转换为pictureVO
     * @param space
     * @return
     */
    @Override
    public SpaceVO objToVo(Space space){
        if(space == null){
            return null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);


        return spaceVO;
    }
}




