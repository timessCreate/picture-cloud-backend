package com.timess.picturecloud.aop;

import com.timess.picturecloud.annotation.AuthCheck;
import com.timess.picturecloud.exception.BusinessException;
import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.model.domain.User;
import com.timess.picturecloud.model.enums.UserRoleEnum;
import com.timess.picturecloud.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        String[] anyRole = authCheck.anyRole();
        //如果都不为空，抛出异常
        if(StringUtils.isNotEmpty(mustRole) && anyRole.length != 0){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "mustRole 和 anyRole至少有一个为空");
        //如果都为空，放行
        }else if(mustRole.isEmpty() && anyRole.length == 0){
            return joinPoint.proceed();
        }
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前用户具有的权限
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 没有用户角色，抛出异常
        if(userRoleEnum == null){
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
        if(anyRole == null){
            UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
            if(mustRoleEnum == null){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "输入的mustRole错误，无匹配的对象");
            }
            //权限角色和用户权限不符合
            if(mustRoleEnum.equals(userRoleEnum)){
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "无权限访问");
            }
        }else{
            boolean access = false;
            for(String role : anyRole){
                UserRoleEnum roleEnum = UserRoleEnum.getEnumByValue(role);
                //如果其中的一个和用户现有角色匹配，则放行
                if(userRoleEnum.equals(roleEnum)){
                    access = true;
                    break;
                }
            }
            if(!access){
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "无权限");
            }
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}
