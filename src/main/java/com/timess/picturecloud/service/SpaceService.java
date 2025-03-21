package com.timess.picturecloud.service;

import com.timess.picturecloud.model.domain.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.dto.space.SpaceAddRequest;

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
}
