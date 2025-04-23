package com.timess.picturecloud.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timess.picturecloud.annotation.AuthCheck;
import com.timess.picturecloud.common.BaseResponse;
import com.timess.picturecloud.common.DeleteRequest;
import com.timess.picturecloud.common.ResultUtils;
import com.timess.picturecloud.constant.UserConstant;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
import com.timess.picturecloud.manager.auth.SpaceUserAuthManager;
import com.timess.picturecloud.model.domain.Space;
import com.timess.picturecloud.model.domain.Space;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.dto.space.SpaceAddRequest;
import com.timess.picturecloud.model.dto.space.SpaceLevel;
import com.timess.picturecloud.model.dto.space.SpaceQueryRequest;
import com.timess.picturecloud.model.dto.space.SpaceUpdateRequest;
import com.timess.picturecloud.model.enums.SpaceLevelEnum;
import com.timess.picturecloud.model.vo.SpaceVO;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/space")
@AllArgsConstructor
public class SpaceController {

    @Autowired
    private final SpaceService spaceService;

    @Autowired
    private SpaceUserAuthManager authManager;

    @Autowired
    private UserService userService;

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        long l = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(l);
    }

    @PostMapping("/update")
    @AuthCheck(anyRole = {UserConstant.SUPER_ADMIN_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
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
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        List<String> permissionList = authManager.getPermissionList(space, userService.getLoginUser(request));
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        spaceVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(spaceVO);
    }
    @GetMapping("/get/vo/userId")
    public BaseResponse<SpaceVO> getSpaceVOByUserId(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        LambdaQueryWrapper<Space> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Space::getUserId, id);
        Space space = spaceService.getOne(lambdaQueryWrapper);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        User loginUser = userService.getLoginUser(request);
        if(!Objects.equals(space.getUserId(), loginUser.getId()) && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "仅空间所有者和管理员可访问");
        }
        // 获取封装类
        return ResultUtils.success(spaceService.objToVo(space));
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
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        log.debug("请求对象信息：" + spaceQueryRequest);
        Page<Space> page = spaceService.page(new Page<>(current, size), spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(page);
    }
}

