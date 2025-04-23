package com.timess.picturecloud.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * @author 33363
 * StpLogic门面类，对项目中的所有stpLogic账号体系进行管理
 */
@Component
public class StpKit {

    public static final String SPACE_TYPE = "space";

    /**
     * 原生会话对象
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    /**
     * 空间会话对象
     */
    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);
}
