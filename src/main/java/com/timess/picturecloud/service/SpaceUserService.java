package com.timess.picturecloud.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.timess.picturecloud.model.domain.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.dto.spaceuser.SpaceUserAddRequest;
import com.timess.picturecloud.model.dto.spaceuser.SpaceUserQueryRequest;
import com.timess.picturecloud.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 33363
* @description 针对表【space_user(空间用户关联表)】的数据库操作Service
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 校验当前数据是否是有效数据
     * @param spaceUser
     * @param b
     */
    void validSpaceUser(SpaceUser spaceUser, boolean b);

    /**
     * 增加空间用户
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 获取响应类
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);
    /**
     * 获取查询条件
     * @param spaceUserQueryRequest
     * @return
     */
    Wrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 实体类列表转化为响应列表
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
