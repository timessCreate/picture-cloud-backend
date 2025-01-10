package com.timess.picturecloud.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.timess.picturecloud.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.timess.picturecloud.model.dto.user.UserQueryRequest;
import com.timess.picturecloud.model.vo.LoginUserVO;
import com.timess.picturecloud.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 33363
* @description 针对表【user(用户)】的数据库操作Service
*/
public interface UserService extends IService<User> {
    /**
     * 注册
     * @param userAccount
     * @param userPassword
     */
    void userRegister(String userAccount, String userPassword);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前用户信息
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 返回脱敏后的当前登录用户信息
     * @param request
     * @return
     */
    LoginUserVO getLoginUserVO(HttpServletRequest request);


    /**
     * 退出登录
     * @param request
     * @return
     */
    Boolean logout(HttpServletRequest request);

    /**
     * 用户信息脱敏
     */
    public UserVO getUserVO(User user);

    /**
     * 用户列表脱敏
     */
    public List<UserVO> getUserVOList(List<User> userList);

    /**
     * 分页查询条件构造
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 是否是管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);
}
