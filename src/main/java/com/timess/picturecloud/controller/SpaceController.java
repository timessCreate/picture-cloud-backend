package com.timess.picturecloud.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timess.picturecloud.annotation.AuthCheck;
import com.timess.picturecloud.common.BaseResponse;
import com.timess.picturecloud.common.DeleteRequest;
import com.timess.picturecloud.common.ResultUtils;
import com.timess.picturecloud.constant.UserConstant;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
import com.timess.picturecloud.model.domain.Picture;
import com.timess.picturecloud.model.domain.Space;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.dto.picture.PictureQueryRequest;
import com.timess.picturecloud.model.dto.space.SpaceLevel;
import com.timess.picturecloud.model.dto.space.SpaceUpdateRequest;
import com.timess.picturecloud.model.enums.SpaceLevelEnum;
import com.timess.picturecloud.service.SpaceService;
import com.timess.picturecloud.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/space")
@AllArgsConstructor
public class SpaceController {

    @Autowired
    private final SpaceService spaceService;

    @Autowired
    private UserService userService;


    @PostMapping("/update")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPER_ADMIN_ROLE})
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 数据校验
        spaceService.validSpace(space, false);
        // 判断是否存在
        long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
    /**
     * 删除空间
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
        // 操作数据库
        oldSpace.setIsDelete(1);
        spaceService.updateById(oldSpace);
        return ResultUtils.success(true);
    }

    /**
     * 获取空间级别列表，便于前端展示
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> getSpaceLevel(){
        List<SpaceLevel> collect = Arrays.stream(SpaceLevelEnum.values())
                .map(s ->
                     new SpaceLevel(
                            s.getValue(),
                            s.getText(),
                            s.getMaxCount(),
                            s.getMaxSize()
                    )
                ).collect(Collectors.toList());
        return ResultUtils.success(collect);
    }

    /**
     * 分页获取空間列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(anyRole = {UserConstant.SUPER_ADMIN_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size));
        return ResultUtils.success(spacePage);
    }
}

