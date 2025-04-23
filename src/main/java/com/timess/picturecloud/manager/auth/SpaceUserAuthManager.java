package com.timess.picturecloud.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.timess.picturecloud.manager.auth.model.SpaceUserAuthConfig;
import com.timess.picturecloud.manager.auth.model.SpaceUserRole;
import com.timess.picturecloud.model.domain.Space;
import com.timess.picturecloud.model.domain.SpaceUser;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.enums.SpaceRoleEnum;
import com.timess.picturecloud.model.enums.SpaceTypeEnum;
import com.timess.picturecloud.service.SpaceUserService;
import com.timess.picturecloud.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 33363
 * 从resources/biz 加载空间权限配置文件，并加载到SpaceUserAuthConfig配置类中
 */
@Component
public class SpaceUserAuthManager {
    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static{
        try {
            String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
            SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("加载权限配置失败", e);
        }
    }
    /**
     * 根据角色获取对应的权限列表
     * @param spaceUserRole 空间角色信息
     * @return 返回该用户拥有的权限
     */
    public List<String> getPermissionsByRole(String spaceUserRole){
        if(StrUtil.isBlank(spaceUserRole)){
            return new ArrayList<>();
        }
        //查找匹配的角色
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r-> spaceUserRole.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if(role == null){
            return new ArrayList<>();
        }
        return role.getPermissions();
    }

    /**
     * 获取权限列表
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser){
        if(loginUser == null){
            return new ArrayList<>();
        }
        //管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        //公共图库
        if(space == null){
            if(userService.isAdmin(loginUser)){
                return ADMIN_PERMISSIONS;
            }
            return new ArrayList<>();
        }
        //获取空间类型信息
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if(spaceTypeEnum == null){
            return new ArrayList<>();
        }
        //根据空间类型获取对应的权限
        switch (spaceTypeEnum){
            case PRIVATE:
                //私有空间， 仅本人或管理员有所有权限
                if(space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)){
                    return ADMIN_PERMISSIONS;
                }else{
                    return new ArrayList<>();
                }
            case TEAM:
                //团队空间，查询spaceUser并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, space.getUserId())
                        .one();
                if(spaceUser == null){
                    return new ArrayList<>();
                }else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }

}
