package com.timess.picturecloud.controller;

import cn.hutool.core.util.ObjUtil;
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
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.dto.user.*;
import com.timess.picturecloud.model.vo.LoginUserVO;
import com.timess.picturecloud.model.vo.UserVO;
import com.timess.picturecloud.service.UserService;
import com.timess.picturecloud.utils.CommonUtils;
import com.timess.picturecloud.utils.EmailApi;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

/**
 * @author 33363
 *
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;


    @Resource
    private EmailApi emailApi;
    /**
     * 用户注册
     * @param userAddRequest 注册登录请求类
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<String> userRegister(@RequestBody UserAddRequest userAddRequest){
        userService.userRegister(userAddRequest.getUserAccount(), userAddRequest.getUserPassword(),userAddRequest.getMail(), userAddRequest.getVerifyCode());
        return ResultUtils.success("注册成功");
    }

    /**
     * 用户登录
     * @param loginRequest 登录请求类
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request){
        LoginUserVO loginUserVO = userService.userLogin(loginRequest.getUserAccount(), loginRequest.getUserPassword(), request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户信息
     * @param request
     * @return
     */
    @GetMapping("/info/get")
    public BaseResponse<LoginUserVO> getLoginUserInfo(HttpServletRequest request){
        LoginUserVO loginUserVO = userService.getLoginUserVO(request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 退出登录
     * @param request
     * @return
     */
   @GetMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request){
       ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
       Boolean result = userService.logout(request);
       return ResultUtils.success(result);
   }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPER_ADMIN_ROLE})
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        //查看数据库中userAccount是否已经被使用
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAddRequest.getUserAccount());
        if(userService.exists(queryWrapper)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已被使用");
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = CommonUtils.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPER_ADMIN_ROLE})
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPER_ADMIN_ROLE})
    public BaseResponse<String> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if(!userService.removeById(deleteRequest.getId())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        return ResultUtils.success("删除成功");
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPER_ADMIN_ROLE})
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE, UserConstant.SUPER_ADMIN_ROLE})
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    @PostMapping("/verifyCode")
    public BaseResponse<Boolean> sendVerifyMail(@RequestBody UserSendRegisterMailRequest mailRequest){
        if(ObjUtil.isEmpty(mailRequest) || StringUtils.isEmpty(mailRequest.getMail())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "传入参数错误");
        }
        boolean result = emailApi.sendGeneralEmail("易图注册验证码", mailRequest.getMail());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "验证码发送失败");
        return ResultUtils.success(true);
    }

}
