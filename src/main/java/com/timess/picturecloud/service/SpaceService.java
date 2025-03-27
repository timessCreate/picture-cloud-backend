package com.timess.picturecloud.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timess.picturecloud.model.domain.Picture;
import com.timess.picturecloud.model.domain.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.dto.space.SpaceAddRequest;
import com.timess.picturecloud.model.dto.space.SpaceQueryRequest;
import com.timess.picturecloud.model.vo.PictureVO;
import com.timess.picturecloud.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 33363
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-03-21 15:20:13
*/
public interface SpaceService extends IService<Space> {

    void validSpace(Space space, boolean b);

    void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建个人空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 将查询请求转化为QueryWrapper对象
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);


    /**
     * 获取spaceVO对象
     * @param space
     * @return
     */
     SpaceVO objToVo(Space space);
}
