package com.timess.picturecloud.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.ThrowUtils;
import com.timess.picturecloud.mapper.UserMapper;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.dto.user.UserQueryRequest;
import com.timess.picturecloud.model.enums.UserRoleEnum;
import com.timess.picturecloud.model.vo.LoginUserVO;
import com.timess.picturecloud.model.vo.UserVO;
import com.timess.picturecloud.service.UserService;
import com.timess.picturecloud.utils.CommonUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.timess.picturecloud.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author 33363
* @description 针对表【user(用户)】的数据库操作Service实现
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    /**
     * 用户注册校验
     * @param userAccount
     * @param userPassword
     */
    @Override
    public void userRegister(String userAccount, String userPassword) {
        //1.校验
        if(StrUtil.hasBlank(userAccount, userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不全");
        }
        //校验账号名是否已经被使用
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号名已被使用");
        //密码加密
        String encryptPassword = CommonUtils.getEncryptPassword(userPassword);
        //数据插入
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "注册失败，数据库异常");
    }

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @return
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        User currentUser = this.getOne(queryWrapper);
        //判定该用户是否存在
        ThrowUtils.throwIf(currentUser == null, ErrorCode.PARAMS_ERROR, "用户不存在");
        //判定密码是否输入正确
        ThrowUtils.throwIf(
                !currentUser.getUserPassword().equals(CommonUtils.getEncryptPassword(userPassword)),
                ErrorCode.PARAMS_ERROR, "密码错误");
        //对象复制
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(currentUser, loginUserVO);
        //保存用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, currentUser);
        return loginUserVO;
    }

    /**
     * 获取当前用户信息
     * @param request 通过请求获取到Session，再次去除当前用户信息
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(currentUser == null || currentUser.getId() == null){
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR,"请登录后访问");
        }
        return currentUser;
    }

    /**
     * 给前端返回脱敏后的用户信息
     * @param request
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(loginUser, loginUserVO);
        return loginUserVO;
    }

    @Override
    public Boolean logout(HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        ThrowUtils.throwIf(currentUser == null, ErrorCode.OPERATION_ERROR, "未登录");
        //移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if(user == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if(CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }


    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

}




